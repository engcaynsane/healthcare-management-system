package com.hms.lab;

import com.hms.audit.AuditService;
import com.hms.common.BranchContext;
import com.hms.common.RefGenerator;
import com.hms.common.enums.LabOrderStatus;
import com.hms.common.exception.BadRequestException;
import com.hms.common.exception.ResourceNotFoundException;
import com.hms.doctor.DoctorScope;
import com.hms.patient.Patient;
import com.hms.security.SecurityUtils;
import com.hms.patient.PatientRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LabService {

    private final LabTestRepository labTestRepository;
    private final LabOrderRepository labOrderRepository;
    private final LabOrderItemRepository labOrderItemRepository;
private final PatientRepository patientRepository;
    private final AuditService auditService;
    private final DoctorScope doctorScope;

    public LabTest createTest(LabTestRequest req) {
        LabTest test = LabTest.builder()
                .code(req.code().trim().toUpperCase())
                .name(req.name())
                .category(req.category())
                .price(req.price())
                .description(req.description())
                .build();
        return labTestRepository.save(test);
    }

    @Transactional
    public LabOrder createOrder(OrderRequest req) {
        Long branchId = BranchContext.branchId();
        Patient patient = patientRepository.findById(req.patientId())
                .orElseThrow(() -> new ResourceNotFoundException("Patient not found"));
        SecurityUtils.requireSameBranch(patient.getBranchId(), "Patient not found");
        LabOrder order = LabOrder.builder()
                .branchId(branchId)
                .orderNumber(RefGenerator.next("LAB"))
                .patient(patient)
                .requestedByUserId(BranchContext.userId())
                .requestedByName(BranchContext.username())
                .status(LabOrderStatus.REQUESTED)
                .build();
        LabOrder saved = labOrderRepository.save(order);
        for (Long testId : req.testIds()) {
            LabTest test = labTestRepository.findById(testId)
                    .orElseThrow(() -> new ResourceNotFoundException("Lab test not found"));
            labOrderItemRepository.save(LabOrderItem.builder()
                    .branchId(branchId)
                    .labOrder(saved)
                    .labTest(test)
                    .price(test.getPrice())
                    .status(LabOrderStatus.REQUESTED)
                    .build());
        }
        auditService.log("LAB_ORDER", "Created lab order " + saved.getOrderNumber());
        return saved;
    }

    @Transactional
    public LabOrderItemView enterResult(Long orderId, ResultRequest req) {
        LabOrder order = labOrderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Lab order not found"));
        SecurityUtils.requireSameBranch(order.getBranchId(), "Lab order not found");
        LabOrderItem item = labOrderItemRepository.findById(req.itemId())
                .orElseThrow(() -> new ResourceNotFoundException("Lab order item not found"));
        if (!item.getLabOrder().getId().equals(order.getId())) {
            throw new BadRequestException("Item does not belong to this order");
        }
        if (order.getStatus() == LabOrderStatus.CANCELLED) {
            throw new BadRequestException("Order is cancelled");
        }
        item.setResult(req.result());
        item.setResultNotes(req.resultNotes());
        item.setStatus(LabOrderStatus.COMPLETED);
        labOrderItemRepository.save(item);
        order.setResultApprovedByUserId(BranchContext.userId());
        order.setStatus(LabOrderStatus.IN_PROGRESS);
        labOrderRepository.save(order);
        auditService.log("LAB_RESULT", "Entered result for " + item.getLabTest().getName());
        return new LabOrderItemView(item.getId(), item.getLabTest().getId(), item.getLabTest().getCode(),
                item.getLabTest().getName(), item.getPrice(), item.getResult(), item.getResultNotes(),
                item.getStatus() != null ? item.getStatus().name() : null);
    }

    @Transactional
    public LabOrder completeOrder(Long orderId) {
        LabOrder order = labOrderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Lab order not found"));
        SecurityUtils.requireSameBranch(order.getBranchId(), "Lab order not found");
        List<LabOrderItem> items = new ArrayList<>(labOrderItemRepository.findByLabOrderId(orderId));
        boolean allDone = !items.isEmpty() && items.stream()
                .allMatch(i -> i.getStatus() == LabOrderStatus.COMPLETED);
        if (!allDone) {
            throw new BadRequestException("All results must be entered before completing the order");
        }
        order.setStatus(LabOrderStatus.COMPLETED);
        order.setCompletedAt(LocalDateTime.now());
        order.setResultApprovedByUserId(BranchContext.userId());
        auditService.log("LAB_COMPLETE", "Completed lab order " + order.getOrderNumber());
        return labOrderRepository.save(order);
    }

    public record LabTestRequest(String code, String name, String category,
                                 BigDecimal price, String description) {
    }

    public record OrderRequest(Long patientId, List<Long> testIds) {
    }

    public record ResultRequest(Long itemId, String result, String resultNotes) {
    }

@Transactional(readOnly = true)
    public Page<LabOrderSummary> listOrders(String status, int page, int size) {
        LabOrderStatus orderStatus = null;
        if (status != null && !status.isBlank()) {
            try {
                orderStatus = LabOrderStatus.valueOf(status.toUpperCase());
            } catch (IllegalArgumentException ignored) {
            }
        }
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        if (doctorScope.isDoctor()) {
            return labOrderRepository.searchRequestedBy(BranchContext.branchId(), BranchContext.userId(),
                            orderStatus, pageable)
                    .map(this::orderSummary);
        }
        return labOrderRepository.search(BranchContext.branchId(), orderStatus, pageable)
                .map(this::orderSummary);
    }

    @Transactional(readOnly = true)
    public LabOrderDetail orderDetail(Long id) {
        LabOrder order = labOrderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Lab order not found"));
        SecurityUtils.requireSameBranch(order.getBranchId(), "Lab order not found");
        if (doctorScope.isDoctor()
                && (order.getRequestedByUserId() == null || !order.getRequestedByUserId().equals(BranchContext.userId()))) {
            throw new ResourceNotFoundException("Lab order not found");
        }
        List<LabOrderItemView> items = labOrderItemRepository.findByLabOrderId(id).stream()
                .map(i -> new LabOrderItemView(i.getId(), i.getLabTest().getId(), i.getLabTest().getCode(),
                        i.getLabTest().getName(), i.getPrice(), i.getResult(), i.getResultNotes(),
                        i.getStatus() != null ? i.getStatus().name() : null))
                .toList();
        return new LabOrderDetail(order.getId(), order.getOrderNumber(),
                order.getPatient().getId(),
                order.getPatient().getFirstName() + " " + order.getPatient().getLastName(),
                order.getStatus() != null ? order.getStatus().name() : null,
                order.getCreatedAt(), order.getRequestedByName(), items);
    }

    private LabOrderSummary orderSummary(LabOrder o) {
        long count = labOrderItemRepository.findByLabOrderId(o.getId()).size();
        return new LabOrderSummary(o.getId(), o.getOrderNumber(),
                o.getPatient().getFirstName() + " " + o.getPatient().getLastName(),
                o.getStatus() != null ? o.getStatus().name() : null,
                o.getCreatedAt(), o.getRequestedByName(), count);
    }

    public record LabOrderSummary(Long id, String orderNumber, String patientName, String status,
                                  LocalDateTime createdAt, String requestedByName, long itemCount) {
    }

    public record LabOrderItemView(Long id, Long testId, String code, String name, BigDecimal price,
                                   String result, String resultNotes, String status) {
    }

    public record LabOrderDetail(Long id, String orderNumber, Long patientId, String patientName,
                                 String status, LocalDateTime createdAt, String requestedByName,
                                 List<LabOrderItemView> items) {
    }
}
