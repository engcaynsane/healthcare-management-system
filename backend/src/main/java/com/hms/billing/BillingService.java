package com.hms.billing;

import com.hms.audit.AuditService;
import com.hms.common.BranchContext;
import com.hms.common.RefGenerator;
import com.hms.common.enums.PaymentMethod;
import com.hms.common.enums.PaymentStatus;
import com.hms.common.exception.BadRequestException;
import com.hms.common.exception.ResourceNotFoundException;
import com.hms.customer.Customer;
import com.hms.customer.CustomerRepository;
import com.hms.patient.Patient;
import com.hms.patient.PatientRepository;
import com.hms.sale.Sale;
import com.hms.sale.SaleRepository;
import com.hms.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BillingService {

    private final InvoiceRepository invoiceRepository;
    private final PaymentRepository paymentRepository;
    private final PatientRepository patientRepository;
    private final CustomerRepository customerRepository;
    private final SaleRepository saleRepository;

    private final AuditService auditService;

    @Transactional
    public InvoiceResponse createInvoice(InvoiceRequest req) {
        Long branchId = BranchContext.branchId();
        BigDecimal subtotal = req.subtotal() == null ? BigDecimal.ZERO : req.subtotal();
        BigDecimal discount = req.discount() == null ? BigDecimal.ZERO : req.discount();
        BigDecimal tax = req.tax() == null ? BigDecimal.ZERO : req.tax();
        BigDecimal total = subtotal.subtract(discount).add(tax);

        Patient patient = null;
        if (req.patientId() != null) {
            patient = patientRepository.findById(req.patientId())
                    .orElseThrow(() -> new ResourceNotFoundException("Patient not found"));
            SecurityUtils.requireSameBranch(patient.getBranchId(), "Patient not found");
        }
        Customer customer = null;
        if (req.customerId() != null) {
            customer = customerRepository.findById(req.customerId())
                    .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));
            SecurityUtils.requireSameBranch(customer.getBranchId(), "Customer not found");
        }
        Sale sale = null;
        if (req.saleId() != null) {
            sale = saleRepository.findById(req.saleId())
                    .orElseThrow(() -> new ResourceNotFoundException("Sale not found"));
            SecurityUtils.requireSameBranch(sale.getBranchId(), "Sale not found");
        }

        Invoice invoice = Invoice.builder()
                .branchId(branchId)
                .invoiceNumber(RefGenerator.next("INV"))
                .patient(patient)
                .customer(customer)
                .sale(sale)
                .description(req.description())
                .subtotal(subtotal.setScale(2, RoundingMode.HALF_UP))
                .discount(discount.setScale(2, RoundingMode.HALF_UP))
                .tax(tax.setScale(2, RoundingMode.HALF_UP))
                .total(total.setScale(2, RoundingMode.HALF_UP))
                .paidAmount(BigDecimal.ZERO)
                .status(PaymentStatus.UNPAID)
                .issuedByUserId(BranchContext.userId())
                .issuedByName(BranchContext.username())
                .build();
        Invoice saved = invoiceRepository.save(invoice);
        auditService.log("INVOICE_CREATE", "Created invoice " + saved.getInvoiceNumber());
        return toResponse(saved);
    }

    @Transactional
    public PaymentResponse recordPayment(Long invoiceId, PaymentRequest req) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found"));
        SecurityUtils.requireSameBranch(invoice.getBranchId(), "Invoice not found");
        BigDecimal newPaid = invoice.getPaidAmount().add(req.amount());
        if (newPaid.compareTo(invoice.getTotal()) > 0) {
            throw new BadRequestException("Amount exceeds outstanding balance");
        }
        invoice.setPaidAmount(newPaid);
        invoice.setStatus(newPaid.compareTo(invoice.getTotal()) >= 0
                ? PaymentStatus.PAID : PaymentStatus.PARTIAL);
        invoiceRepository.save(invoice);

        Payment payment = Payment.builder()
                .branchId(invoice.getBranchId())
                .invoice(invoice)
                .amount(req.amount().setScale(2, RoundingMode.HALF_UP))
                .method(req.method() != null ? PaymentMethod.valueOf(req.method()) : PaymentMethod.CASH)
                .reference(req.reference())
                .paidAt(LocalDateTime.now())
                .receivedByUserId(BranchContext.userId())
                .receivedByName(BranchContext.username())
                .build();
        Payment saved = paymentRepository.save(payment);
        auditService.log("PAYMENT", "Recorded payment " + req.amount() + " on invoice " + invoice.getInvoiceNumber());
        return new PaymentResponse(saved.getId(), saved.getAmount(),
                saved.getMethod() != null ? saved.getMethod().name() : null,
                saved.getReference(), saved.getPaidAt(), saved.getInvoice().getId());
    }

    @Transactional(readOnly = true)
    public Page<InvoiceSummary> search(LocalDate date, String status, int page, int size) {
        PaymentStatus paymentStatus = null;
        if (status != null && !status.isBlank()) {
            try {
                paymentStatus = PaymentStatus.valueOf(status.toUpperCase());
            } catch (IllegalArgumentException ignored) {
            }
        }
        return invoiceRepository.search(BranchContext.branchId(), date, paymentStatus,
                        PageRequest.of(page, size, Sort.by("createdAt").descending()))
                .map(this::summary);
    }

    @Transactional(readOnly = true)
    public InvoiceDetail detail(Long id) {
        Invoice invoice = invoiceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found"));
        SecurityUtils.requireSameBranch(invoice.getBranchId(), "Invoice not found");
        List<PaymentResponse> payments = paymentRepository.findByInvoiceId(id).stream()
                .map(p -> new PaymentResponse(p.getId(), p.getAmount(),
                        p.getMethod() != null ? p.getMethod().name() : null,
                        p.getReference(), p.getPaidAt(), id))
                .toList();
        return new InvoiceDetail(invoice.getId(), invoice.getInvoiceNumber(),
                invoice.getPatient() != null ? invoice.getPatient().getFirstName() + " " + invoice.getPatient().getLastName() : null,
                invoice.getCustomer() != null ? invoice.getCustomer().getName() : null,
                invoice.getDescription(), invoice.getSubtotal(), invoice.getDiscount(), invoice.getTax(),
                invoice.getTotal(), invoice.getPaidAmount(),
                invoice.getStatus() != null ? invoice.getStatus().name() : null,
                invoice.getIssuedByName(), invoice.getCreatedAt(), payments);
    }

    private InvoiceSummary summary(Invoice i) {
        return new InvoiceSummary(i.getId(), i.getInvoiceNumber(),
                i.getPatient() != null ? i.getPatient().getFirstName() + " " + i.getPatient().getLastName() : null,
                i.getCustomer() != null ? i.getCustomer().getName() : null,
                i.getDescription(), i.getTotal(), i.getPaidAmount(),
                i.getStatus() != null ? i.getStatus().name() : null, i.getCreatedAt(), i.getIssuedByName());
    }

    private InvoiceResponse toResponse(Invoice i) {
        return new InvoiceResponse(i.getId(), i.getInvoiceNumber(),
                i.getDescription(), i.getSubtotal(), i.getDiscount(), i.getTax(), i.getTotal(),
                i.getPaidAmount(), i.getStatus() != null ? i.getStatus().name() : null,
                i.getIssuedByName(), i.getCreatedAt());
    }

    public record InvoiceResponse(Long id, String invoiceNumber, String description,
                                  BigDecimal subtotal, BigDecimal discount, BigDecimal tax, BigDecimal total,
                                  BigDecimal paidAmount, String status, String issuedByName,
                                  LocalDateTime createdAt) {
    }

    public record InvoiceSummary(Long id, String invoiceNumber, String patientName, String customerName,
                                 String description, BigDecimal total, BigDecimal paidAmount, String status,
                                 LocalDateTime createdAt, String issuedByName) {
    }

    public record PaymentResponse(Long id, BigDecimal amount, String method, String reference,
                                  LocalDateTime paidAt, Long invoiceId) {
    }

    public record InvoiceDetail(Long id, String invoiceNumber, String patientName, String customerName,
                                String description, BigDecimal subtotal, BigDecimal discount, BigDecimal tax,
                                BigDecimal total, BigDecimal paidAmount, String status, String issuedByName,
                                LocalDateTime createdAt, List<PaymentResponse> payments) {
    }

    public record InvoiceRequest(Long patientId, Long customerId, Long saleId,
                                 String description, BigDecimal subtotal,
                                 BigDecimal discount, BigDecimal tax) {
    }

    public record PaymentRequest(BigDecimal amount, String method, String reference) {
    }
}