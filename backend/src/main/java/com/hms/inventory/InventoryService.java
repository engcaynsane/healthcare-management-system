package com.hms.inventory;

import com.hms.audit.AuditService;
import com.hms.common.BranchContext;
import com.hms.common.RefGenerator;
import com.hms.common.enums.StockMovementType;
import com.hms.common.enums.TransferStatus;
import com.hms.common.exception.BadRequestException;
import com.hms.common.exception.ResourceNotFoundException;
import com.hms.branch.BranchRepository;
import com.hms.pharmacy.Medicine;
import com.hms.pharmacy.MedicineRepository;
import com.hms.supplier.Supplier;
import com.hms.supplier.SupplierRepository;
import com.hms.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class InventoryService {

    private final InventoryRepository inventoryRepository;
    private final MedicineBatchRepository batchRepository;
    private final StockMovementRepository movementRepository;
    private final InventoryTransferRepository transferRepository;
    private final MedicineRepository medicineRepository;
    private final SupplierRepository supplierRepository;
    private final BranchRepository branchRepository;
    private final AuditService auditService;

    @Transactional
    public ReceiveResult receive(ReceiveRequest req) {
        Long branchId = BranchContext.branchId();
        if (req.batchNo() == null || req.batchNo().isBlank()) {
            throw new BadRequestException("Batch number is required");
        }
        if (req.expiryDate() == null) {
            throw new BadRequestException("Expiry date is required");
        }
        if (req.quantity() == null || req.quantity() <= 0) {
            throw new BadRequestException("Quantity must be positive");
        }
        Medicine medicine = medicineRepository.findById(req.medicineId())
                .orElseThrow(() -> new ResourceNotFoundException("Medicine not found"));

        MedicineBatch batch = batchRepository
                .findByMedicineIdAndBatchNoAndBranchId(medicine.getId(), req.batchNo(), branchId)
                .orElseGet(() -> batchRepository.save(MedicineBatch.builder()
                        .medicine(medicine)
                        .batchNo(req.batchNo())
                        .branchId(branchId)
                        .expiryDate(req.expiryDate())
                        .purchaseDate(req.purchaseDate() != null ? req.purchaseDate() : LocalDate.now())
                        .costPrice(req.costPrice())
                        .build()));

        if (req.supplierId() != null) {
            Supplier supplier = supplierRepository.findById(req.supplierId()).orElse(null);
            batch.setSupplier(supplier);
        }
        int before = batch.getQuantity();
        int after = before + req.quantity();
        batch.setInitialQuantity(batch.getInitialQuantity() + req.quantity());
        batch.setQuantity(after);
        batchRepository.save(batch);

        Inventory inv = inventoryRepository.findByBranchIdAndMedicineId(branchId, medicine.getId())
                .stream().filter(i -> i.getBatch().getId().equals(batch.getId())).findFirst()
                .orElseGet(() -> inventoryRepository.save(Inventory.builder()
                        .branchId(branchId)
                        .medicine(medicine)
                        .batch(batch)
                        .location(req.location())
                        .build()));
        inv.setQuantity(inv.getQuantity() + req.quantity());
        if (req.location() != null) {
            inv.setLocation(req.location());
        }
        inventoryRepository.save(inv);

        movementRepository.save(StockMovement.builder()
                .branchId(branchId)
                .type(StockMovementType.RECEIVE)
                .medicine(medicine)
                .batch(batch)
                .quantityChange(req.quantity())
                .beforeQty(before)
                .afterQty(after)
                .reference(req.reference())
                .build());

        auditService.log("STOCK_RECEIVE", "Received " + req.quantity() + " x " + medicine.getName() +
                " (batch " + req.batchNo() + ")");
        return new ReceiveResult(batch.getId(), batch.getBatchNo(), batch.getQuantity(),
                medicine.getName(), batch.getExpiryDate());
    }

    @Transactional
    public void adjust(AdjustRequest req) {
        Long branchId = BranchContext.branchId();
        Medicine medicine = medicineRepository.findById(req.medicineId())
                .orElseThrow(() -> new ResourceNotFoundException("Medicine not found"));
        Inventory inv = null;
        if (req.batchId() != null) {
            inv = inventoryRepository.findByBranchIdAndMedicineId(branchId, medicine.getId())
                    .stream().filter(i -> i.getBatch().getId().equals(req.batchId())).findFirst()
                    .orElseThrow(() -> new ResourceNotFoundException("Inventory record not found"));
        }
        if (inv == null) {
            inv = inventoryRepository.findByBranchIdAndMedicineId(branchId, medicine.getId())
                    .stream().findFirst()
                    .orElseThrow(() -> new BadRequestException("No stock record for this medicine"));
        }
        int before = inv.getQuantity();
        int delta = req.quantityChange();
        int after = before + delta;
        if (after < 0) {
            throw new BadRequestException("Insufficient stock");
        }
        inv.setQuantity(after);
        inventoryRepository.save(inv);
        inv.getBatch().setQuantity(after);
        batchRepository.save(inv.getBatch());

        StockMovementType type = req.type() != null ? StockMovementType.valueOf(req.type()) : StockMovementType.ADJUST;
        movementRepository.save(StockMovement.builder()
                .branchId(branchId)
                .type(type)
                .medicine(medicine)
                .batch(inv.getBatch())
                .quantityChange(delta)
                .beforeQty(before)
                .afterQty(after)
                .reference(req.reference())
                .reason(req.reason())
                .build());
        auditService.log("STOCK_ADJUST", "Adjusted " + medicine.getName() + " by " + delta +
                " (" + type + ")");
    }

    @Transactional(readOnly = true)
    public List<StockRow> stock(Long branchId) {
        Map<Long, StockRow> rows = new HashMap<>();
        for (Inventory i : inventoryRepository.findAllByBranchId(branchId)) {
            Medicine m = i.getMedicine();
            StockRow row = rows.computeIfAbsent(m.getId(), k -> new StockRow(
                    m.getId(), m.getName(), m.getGenericName(), m.getBarcode(),
                    m.getSellingPrice(), m.getCostPrice(), m.getReorderLevel(), m.getUnit(),
                    m.isActive()));
            row.totalQty += i.getQuantity();
            if (m.getReorderLevel() > 0 && i.getQuantity() > 0) {
                row.batches++;
            }
            row.batchList.add(new BatchRow(i.getBatch().getId(), i.getBatch().getBatchNo(),
                    i.getBatch().getExpiryDate(), i.getQuantity()));
        }
        for (StockRow r : rows.values()) {
            r.expiringSoon = r.batchList.stream().anyMatch(b -> b.expiryDate() != null
                    && b.expiryDate().isBefore(LocalDate.now().plusDays(30)));
            r.lowStock = r.reorderLevel > 0 && r.totalQty <= r.reorderLevel;
        }
        return rows.values().stream().toList();
    }

    @Transactional(readOnly = true)
    public List<StockRow> lowStock(Long branchId) {
        return stock(branchId).stream().filter(r -> r.lowStock).toList();
    }

    @Transactional(readOnly = true)
    public List<BatchRow> expiring(Long branchId, int withinDays) {
        LocalDate limit = LocalDate.now().plusDays(withinDays);
        return inventoryRepository.findExpiring(branchId, limit).stream()
                .map(i -> new BatchRow(i.getBatch().getId(), i.getBatch().getBatchNo(),
                        i.getBatch().getExpiryDate(), i.getQuantity()))
                .toList();
    }

    @Transactional(readOnly = true)
    public int pendingTransfers(Long branchId) {
        return (int) transferRepository.search(branchId, null,
                        org.springframework.data.domain.Pageable.unpaged()).get().map(t -> t)
                .filter(t -> t.getStatus() != TransferStatus.RECEIVED
                        && t.getStatus() != TransferStatus.REJECTED).count();
    }

    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<MovementResponse> movements(int page, int size) {
        return movementRepository.findByBranchId(BranchContext.branchId(),
                        org.springframework.data.domain.PageRequest.of(page, size,
                                org.springframework.data.domain.Sort.by("createdAt").descending()))
                .map(m -> new MovementResponse(m.getId(),
                        m.getType() != null ? m.getType().name() : null,
                        m.getMedicine() != null ? m.getMedicine().getName() : null,
                        m.getMedicine() != null ? m.getMedicine().getId() : null,
                        m.getBatch() != null ? m.getBatch().getBatchNo() : null,
                        m.getQuantityChange(), m.getBeforeQty(), m.getAfterQty(),
                        m.getReference(), m.getReason(), m.getCreatedAt()));
    }

    @Transactional
    public TransferResponse requestTransfer(TransferRequest req) {
        Long branchId = BranchContext.branchId();
        if (!branchRepository.existsById(req.toBranchId())) {
            throw new BadRequestException("Destination branch not found");
        }
        Medicine medicine = medicineRepository.findById(req.medicineId())
                .orElseThrow(() -> new ResourceNotFoundException("Medicine not found"));
        Inventory inv = inventoryRepository.findByBranchIdAndMedicineId(branchId, medicine.getId())
                .stream().filter(i -> i.getQuantity() >= req.quantity()).findFirst()
                .orElseThrow(() -> new BadRequestException("Insufficient stock for transfer"));
        InventoryTransfer transfer = InventoryTransfer.builder()
                .branchId(branchId)
                .transferNumber(RefGenerator.next("TRF"))
                .toBranchId(req.toBranchId())
                .medicine(medicine)
                .batch(inv.getBatch())
                .quantity(req.quantity())
                .status(TransferStatus.PENDING)
                .requestedBy(BranchContext.userId())
                .reason(req.reason())
                .build();
        InventoryTransfer saved = transferRepository.save(transfer);
        auditService.log("TRANSFER_REQUEST", "Requested transfer of " + req.quantity() + " x " +
                medicine.getName() + " to branch " + req.toBranchId());
        return toResponse(saved);
    }

    @Transactional
    public TransferResponse approveTransfer(Long id) {
        InventoryTransfer t = get(id);
        SecurityUtils.requireSameBranch(t.getBranchId(), "Transfer not found");
        if (t.getStatus() != TransferStatus.PENDING) {
            throw new BadRequestException("Transfer is not pending");
        }
        t.setStatus(TransferStatus.APPROVED);
        t.setApprovedBy(BranchContext.userId());
        return toResponse(transferRepository.save(t));
    }

    @Transactional
    public TransferResponse rejectTransfer(Long id, String reason) {
        InventoryTransfer t = get(id);
        SecurityUtils.requireSameBranch(t.getBranchId(), "Transfer not found");
        if (t.getStatus() != TransferStatus.PENDING) {
            throw new BadRequestException("Transfer is not pending");
        }
        t.setStatus(TransferStatus.REJECTED);
        t.setReason(reason != null ? reason : t.getReason());
        return toResponse(transferRepository.save(t));
    }

    @Transactional
    public TransferResponse shipTransfer(Long id) {
        InventoryTransfer t = get(id);
        SecurityUtils.requireSameBranch(t.getBranchId(), "Transfer not found");
        if (t.getStatus() != TransferStatus.APPROVED) {
            throw new BadRequestException("Transfer must be approved before shipping");
        }
        Long branchId = BranchContext.branchId();
        Inventory inv = inventoryRepository.findByBranchIdAndMedicineId(branchId, t.getMedicine().getId())
                .stream().filter(i -> i.getBatch().getId().equals(t.getBatch().getId())).findFirst()
                .orElseThrow(() -> new BadRequestException("No stock record found"));
        if (inv.getQuantity() < t.getQuantity()) {
            throw new BadRequestException("Insufficient stock");
        }
        int before = inv.getQuantity();
        int after = before - t.getQuantity();
        inv.setQuantity(after);
        inventoryRepository.save(inv);
        t.getBatch().setQuantity(after);
        batchRepository.save(t.getBatch());
        movementRepository.save(StockMovement.builder()
                .branchId(branchId)
                .type(StockMovementType.TRANSFER_OUT)
                .medicine(t.getMedicine())
                .batch(t.getBatch())
                .quantityChange(-t.getQuantity())
                .beforeQty(before)
                .afterQty(after)
                .reference(t.getTransferNumber())
                .build());
        t.setStatus(TransferStatus.IN_TRANSIT);
        t.setShippedBy(BranchContext.userId());
        auditService.log("TRANSFER_SHIP", "Shipped transfer " + t.getTransferNumber());
        return toResponse(transferRepository.save(t));
    }

    @Transactional
    public TransferResponse receiveTransfer(Long id) {
        InventoryTransfer t = get(id);
        if (!SecurityUtils.isSuperAdmin()) {
            Long current = BranchContext.branchId();
            if (t.getToBranchId() == null || !t.getToBranchId().equals(current)) {
                throw new ResourceNotFoundException("Transfer not found");
            }
        }
        if (t.getStatus() != TransferStatus.IN_TRANSIT) {
            throw new BadRequestException("Transfer is not in transit");
        }
        Long branchId = BranchContext.branchId();
        MedicineBatch batch = batchRepository
                .findByMedicineIdAndBatchNoAndBranchId(t.getMedicine().getId(), t.getBatch().getBatchNo(), branchId)
                .orElseGet(() -> batchRepository.save(MedicineBatch.builder()
                        .medicine(t.getMedicine())
                        .batchNo(t.getBatch().getBatchNo())
                        .branchId(branchId)
                        .expiryDate(t.getBatch().getExpiryDate())
                        .purchaseDate(LocalDate.now())
                        .costPrice(t.getBatch().getCostPrice())
                        .quantity(0)
                        .initialQuantity(0)
                        .build()));
        batch.setQuantity(batch.getQuantity() + t.getQuantity());
        batch.setInitialQuantity(batch.getInitialQuantity() + t.getQuantity());
        batchRepository.save(batch);

        Inventory inv = inventoryRepository.findByBranchIdAndMedicineId(branchId, t.getMedicine().getId())
                .stream().filter(i -> i.getBatch().getId().equals(batch.getId())).findFirst()
                .orElseGet(() -> inventoryRepository.save(Inventory.builder()
                        .branchId(branchId)
                        .medicine(t.getMedicine())
                        .batch(batch)
                        .build()));
        inv.setQuantity(inv.getQuantity() + t.getQuantity());
        inventoryRepository.save(inv);
        movementRepository.save(StockMovement.builder()
                .branchId(branchId)
                .type(StockMovementType.TRANSFER_IN)
                .medicine(t.getMedicine())
                .batch(batch)
                .quantityChange(t.getQuantity())
                .reference(t.getTransferNumber())
                .build());
        t.setStatus(TransferStatus.RECEIVED);
        t.setReceivedBy(BranchContext.userId());
        auditService.log("TRANSFER_RECEIVE", "Received transfer " + t.getTransferNumber());
        return toResponse(transferRepository.save(t));
    }

    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<TransferResponse> listTransfers(String status,
                                                                                int page, int size) {
        TransferStatus transferStatus = null;
        if (status != null && !status.isBlank()) {
            try {
                transferStatus = TransferStatus.valueOf(status.toUpperCase());
            } catch (IllegalArgumentException ignored) {
            }
        }
        return transferRepository.search(BranchContext.branchId(), transferStatus,
                        org.springframework.data.domain.PageRequest.of(page, size,
                                org.springframework.data.domain.Sort.by("createdAt").descending()))
                .map(this::toResponse);
    }

    private TransferResponse toResponse(InventoryTransfer t) {
        return new TransferResponse(t.getId(), t.getTransferNumber(), t.getBranchId(), t.getToBranchId(),
                t.getMedicine().getId(), t.getMedicine().getName(),
                t.getBatch() != null ? t.getBatch().getBatchNo() : null,
                t.getQuantity(), t.getStatus() != null ? t.getStatus().name() : null,
                t.getReason(), t.getCreatedAt());
    }

    private InventoryTransfer get(Long id) {
        return transferRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Transfer not found"));
    }

    public record ReceiveRequest(Long medicineId, String batchNo, LocalDate expiryDate,
                                 LocalDate purchaseDate, BigDecimal costPrice,
                                 Integer quantity, Long supplierId, String location, String reference) {
    }

    public record AdjustRequest(Long medicineId, Long batchId, int quantityChange,
                                String type, String reason, String reference) {
    }

    public record TransferRequest(Long medicineId, Long toBranchId, int quantity, String reason) {
    }

    public record ReceiveResult(Long batchId, String batchNo, int quantity, String medicineName,
                                LocalDate expiryDate) {
    }

    public record TransferResponse(Long id, String transferNumber, Long fromBranchId, Long toBranchId,
                                   Long medicineId, String medicineName, String batchNo, int quantity,
                                   String status, String reason, java.time.LocalDateTime createdAt) {
    }

    public record MovementResponse(Long id, String type, String medicineName, Long medicineId,
                                   String batchNo, int quantityChange, int beforeQty, int afterQty,
                                   String reference, String reason, java.time.LocalDateTime createdAt) {
    }

    public record BatchRow(Long batchId, String batchNo, LocalDate expiryDate, int quantity) {
    }

    public static class StockRow {
        public Long medicineId;
        public String name;
        public String genericName;
        public String barcode;
        public BigDecimal sellingPrice;
        public BigDecimal costPrice;
        public int reorderLevel;
        public String unit;
        public boolean active;
        public int totalQty;
        public int batches;
        public boolean expiringSoon;
        public boolean lowStock;
        public List<BatchRow> batchList = new ArrayList<>();

        public StockRow(Long medicineId, String name, String genericName, String barcode,
                        BigDecimal sellingPrice, BigDecimal costPrice, int reorderLevel, String unit,
                        boolean active) {
            this.medicineId = medicineId;
            this.name = name;
            this.genericName = genericName;
            this.barcode = barcode;
            this.sellingPrice = sellingPrice;
            this.costPrice = costPrice;
            this.reorderLevel = reorderLevel;
            this.unit = unit;
            this.active = active;
        }
    }
}