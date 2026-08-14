package com.hms.inventory;

import com.hms.common.BranchScopedEntity;
import com.hms.common.enums.TransferStatus;
import com.hms.pharmacy.Medicine;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "inventory_transfers")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class InventoryTransfer extends BranchScopedEntity {

    @Column(nullable = false, unique = true, length = 40)
    private String transferNumber;

    @Column(nullable = false)
    private Long toBranchId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "medicine_id")
    private Medicine medicine;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "batch_id")
    private MedicineBatch batch;

    @Column(nullable = false)
    private int quantity;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    @Builder.Default
    private TransferStatus status = TransferStatus.PENDING;

    private Long requestedBy;

    private Long approvedBy;

    private Long shippedBy;

    private Long receivedBy;

    @Column(columnDefinition = "text")
    private String reason;
}