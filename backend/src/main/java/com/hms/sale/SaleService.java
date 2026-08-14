package com.hms.sale;

import com.hms.audit.AuditService;
import com.hms.common.BranchContext;
import com.hms.common.RefGenerator;
import com.hms.common.enums.PaymentMethod;
import com.hms.common.enums.SaleStatus;
import com.hms.common.enums.StockMovementType;
import com.hms.common.exception.BadRequestException;
import com.hms.common.exception.ResourceNotFoundException;
import com.hms.customer.Customer;
import com.hms.security.SecurityUtils;
import com.hms.customer.CustomerRepository;
import com.hms.inventory.Inventory;
import com.hms.inventory.InventoryRepository;
import com.hms.inventory.MedicineBatch;
import com.hms.inventory.MedicineBatchRepository;
import com.hms.inventory.StockMovement;
import com.hms.inventory.StockMovementRepository;
import com.hms.patient.Patient;
import com.hms.patient.PatientRepository;
import com.hms.pharmacy.Medicine;
import com.hms.pharmacy.MedicineRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SaleService {

    private final SaleRepository saleRepository;
    private final SaleItemRepository saleItemRepository;
    private final MedicineRepository medicineRepository;
    private final InventoryRepository inventoryRepository;
    private final MedicineBatchRepository batchRepository;
    private final StockMovementRepository movementRepository;
    private final CustomerRepository customerRepository;
    private final PatientRepository patientRepository;
    private final AuditService auditService;

    @Transactional
    public Sale create(SaleRequest req) {
        Long branchId = BranchContext.branchId();
        List<SaleItem> items = new ArrayList<>();
        BigDecimal subtotal = BigDecimal.ZERO;

        for (Item item : req.items()) {
            if (item.quantity() <= 0) {
                throw new BadRequestException("Quantity must be positive");
            }
            Medicine medicine = medicineRepository.findById(item.medicineId())
                    .orElseThrow(() -> new ResourceNotFoundException("Medicine not found"));
            BigDecimal unitPrice = item.unitPrice() != null ? item.unitPrice() : medicine.getSellingPrice();
            if (unitPrice == null) {
                throw new BadRequestException("No price set for medicine " + medicine.getName());
            }

            int remaining = item.quantity();
            List<MedicineBatch> batches;
            if (item.batchId() != null) {
                MedicineBatch selectedBatch = batchRepository.findById(item.batchId())
                        .orElseThrow(() -> new ResourceNotFoundException("Batch not found"));
                SecurityUtils.requireSameBranch(selectedBatch.getBranchId(), "Batch not found");
                batches = List.of(selectedBatch);
            } else {
                batches = batchRepository.findForFefo(medicine.getId(), branchId);
            }

            List<Inventory> invs = inventoryRepository.findByBranchIdAndMedicineId(branchId, medicine.getId());
            for (MedicineBatch batch : batches) {
                if (remaining <= 0) {
                    break;
                }
                Inventory inv = invs.stream()
                        .filter(i -> i.getBatch().getId().equals(batch.getId()))
                        .findFirst().orElse(null);
                if (inv == null || inv.getQuantity() <= 0) {
                    continue;
                }
                int taken = Math.min(remaining, inv.getQuantity());
                int before = inv.getQuantity();
                int after = before - taken;
                inv.setQuantity(after);
                inventoryRepository.save(inv);
                batch.setQuantity(after);
                batchRepository.save(batch);
                movementRepository.save(StockMovement.builder()
                        .branchId(branchId)
                        .type(StockMovementType.SALE)
                        .medicine(medicine)
                        .batch(batch)
                        .quantityChange(-taken)
                        .beforeQty(before)
                        .afterQty(after)
                        .build());

                BigDecimal lineTotal = unitPrice.multiply(BigDecimal.valueOf(taken));
                SaleItem si = SaleItem.builder()
                        .branchId(branchId)
                        .medicine(medicine)
                        .batch(batch)
                        .quantity(taken)
                        .unitPrice(unitPrice)
                        .lineTotal(lineTotal)
                        .build();
                items.add(si);
                subtotal = subtotal.add(lineTotal);
                remaining -= taken;
            }
            if (remaining > 0) {
                throw new BadRequestException("Insufficient stock for " + medicine.getName());
            }
        }

        BigDecimal discount = req.discount() == null ? BigDecimal.ZERO : req.discount();
        BigDecimal tax = req.tax() == null ? BigDecimal.ZERO : req.tax();
        BigDecimal total = subtotal.subtract(discount).add(tax);
        BigDecimal paid = req.paidAmount() == null ? total : req.paidAmount();
        if (paid.compareTo(total) < 0) {
            throw new BadRequestException("Paid amount is less than total");
        }
        BigDecimal change = paid.subtract(total);

        PaymentMethod method = req.paymentMethod() == null
                ? PaymentMethod.CASH : PaymentMethod.valueOf(req.paymentMethod());

        Customer customer = null;
        if (req.customerId() != null) {
            customer = customerRepository.findById(req.customerId())
                    .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));
            SecurityUtils.requireSameBranch(customer.getBranchId(), "Customer not found");
            customer.setLoyaltyPoints(customer.getLoyaltyPoints() + 1);
            customerRepository.save(customer);
        }
        Patient patient = null;
        if (req.patientId() != null) {
            patient = patientRepository.findById(req.patientId())
                    .orElseThrow(() -> new ResourceNotFoundException("Patient not found"));
            SecurityUtils.requireSameBranch(patient.getBranchId(), "Patient not found");
        }

        Sale sale = Sale.builder()
                .branchId(branchId)
                .saleNumber(RefGenerator.next("SALE"))
                .customer(customer)
                .patient(patient)
                .subtotal(subtotal.setScale(2, RoundingMode.HALF_UP))
                .discount(discount.setScale(2, RoundingMode.HALF_UP))
                .tax(tax.setScale(2, RoundingMode.HALF_UP))
                .total(total.setScale(2, RoundingMode.HALF_UP))
                .paidAmount(paid.setScale(2, RoundingMode.HALF_UP))
                .changeAmount(change.setScale(2, RoundingMode.HALF_UP))
                .paymentMethod(method)
                .status(SaleStatus.COMPLETED)
                .cashierUserId(BranchContext.userId())
                .cashierName(BranchContext.username())
                .note(req.note())
                .build();
        Sale saved = saleRepository.save(sale);
        for (SaleItem si : items) {
            si.setSale(saved);
            saleItemRepository.save(si);
        }
        auditService.log("SALE_CREATE", "Sale " + saved.getSaleNumber() + " total " + saved.getTotal());
        return saved;
    }

    @Transactional
    public Sale refund(Long saleId, String reason) {
        Long branchId = BranchContext.branchId();
        Sale sale = saleRepository.findById(saleId)
                .orElseThrow(() -> new ResourceNotFoundException("Sale not found"));
        SecurityUtils.requireSameBranch(sale.getBranchId(), "Sale not found");
        if (sale.getStatus() != SaleStatus.COMPLETED) {
            throw new BadRequestException("Sale already refunded or voided");
        }
        List<SaleItem> items = saleItemRepository.findBySaleId(sale.getId());
        for (SaleItem item : items) {
            if (item.isRefunded()) {
                continue;
            }
            int before = item.getBatch().getQuantity();
            int after = before + item.getQuantity();
            item.getBatch().setQuantity(after);
            batchRepository.save(item.getBatch());
            Inventory inv = inventoryRepository.findByBranchIdAndMedicineId(branchId, item.getMedicine().getId())
                    .stream().filter(i -> i.getBatch().getId().equals(item.getBatch().getId())).findFirst()
                    .orElseGet(() -> inventoryRepository.save(Inventory.builder()
                            .branchId(branchId)
                            .medicine(item.getMedicine())
                            .batch(item.getBatch())
                            .quantity(0)
                            .build()));
            inv.setQuantity(inv.getQuantity() + item.getQuantity());
            inventoryRepository.save(inv);
            movementRepository.save(StockMovement.builder()
                    .branchId(branchId)
                    .type(StockMovementType.SALE_REFUND)
                    .medicine(item.getMedicine())
                    .batch(item.getBatch())
                    .quantityChange(item.getQuantity())
                    .beforeQty(before)
                    .afterQty(after)
                    .reference(sale.getSaleNumber())
                    .reason(reason)
                    .build());
            item.setRefunded(true);
            saleItemRepository.save(item);
        }
        sale.setStatus(SaleStatus.REFUNDED);
        auditService.log("SALE_REFUND", "Refunded sale " + sale.getSaleNumber() + " (" + reason + ")");
        return saleRepository.save(sale);
    }

    public record Item(Long medicineId, Long batchId, int quantity, BigDecimal unitPrice) {
    }

    public record SaleRequest(List<Item> items, Long customerId, Long patientId,
                              BigDecimal discount, BigDecimal tax, BigDecimal paidAmount,
                              String paymentMethod, String note) {
    }

    @Transactional(readOnly = true)
    public Page<SaleSummary> search(LocalDate date, String status, String q, int page, int size) {
        SaleStatus saleStatus = null;
        if (status != null && !status.isBlank()) {
            try {
                saleStatus = SaleStatus.valueOf(status.toUpperCase());
            } catch (IllegalArgumentException ignored) {
            }
        }
        return saleRepository.search(BranchContext.branchId(), date, saleStatus, q,
                        PageRequest.of(page, size, Sort.by("createdAt").descending()))
                .map(this::summary);
    }

    @Transactional(readOnly = true)
    public SaleDetail detail(Long id) {
        Sale sale = saleRepository.findById(id)
                .orElseThrow(() -> new com.hms.common.exception.ResourceNotFoundException("Sale not found"));
        SecurityUtils.requireSameBranch(sale.getBranchId(), "Sale not found");
        List<SaleItemResponse> items = saleItemRepository.findBySaleId(sale.getId()).stream()
                .map(i -> new SaleItemResponse(i.getId(), i.getMedicine().getName(),
                        i.getBatch() != null ? i.getBatch().getBatchNo() : null,
                        i.getQuantity(), i.getUnitPrice(), i.getLineTotal(), i.isRefunded()))
                .toList();
        return new SaleDetail(sale.getId(), sale.getSaleNumber(), sale.getCreatedAt(),
                sale.getCustomer() != null ? sale.getCustomer().getName() : null,
                sale.getPatient() != null ? (sale.getPatient().getFirstName() + " " + sale.getPatient().getLastName()) : null,
                sale.getSubtotal(), sale.getDiscount(), sale.getTax(), sale.getTotal(),
                sale.getPaidAmount(), sale.getChangeAmount(),
                sale.getPaymentMethod() != null ? sale.getPaymentMethod().name() : null,
                sale.getStatus() != null ? sale.getStatus().name() : null,
                sale.getCashierName(), sale.getNote(), items);
    }

    private SaleSummary summary(Sale s) {
        return new SaleSummary(s.getId(), s.getSaleNumber(), s.getCreatedAt(),
                s.getCustomer() != null ? s.getCustomer().getName() : null,
                s.getPatient() != null ? (s.getPatient().getFirstName() + " " + s.getPatient().getLastName()) : null,
                s.getTotal(), s.getPaidAmount(), s.getPaymentMethod() != null ? s.getPaymentMethod().name() : null,
                s.getStatus() != null ? s.getStatus().name() : null, s.getCashierName());
    }

    public record SaleSummary(Long id, String saleNumber, java.time.LocalDateTime createdAt,
                              String customerName, String patientName, BigDecimal total,
                              BigDecimal paidAmount, String paymentMethod, String status, String cashierName) {
    }

    public record SaleItemResponse(Long id, String medicineName, String batchNo, int quantity,
                                   BigDecimal unitPrice, BigDecimal lineTotal, boolean refunded) {
    }

    public record SaleDetail(Long id, String saleNumber, java.time.LocalDateTime createdAt,
                             String customerName, String patientName, BigDecimal subtotal,
                             BigDecimal discount, BigDecimal tax, BigDecimal total, BigDecimal paidAmount,
                             BigDecimal changeAmount, String paymentMethod, String status,
                             String cashierName, String note, List<SaleItemResponse> items) {
    }
}