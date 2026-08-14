package com.hms.inventory;

import com.hms.common.BranchScopedEntity;
import com.hms.common.enums.StockMovementType;
import com.hms.pharmacy.Medicine;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "stock_movements", indexes = @Index(name = "idx_sm_branch", columnList = "branch_id"))
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class StockMovement extends BranchScopedEntity {

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StockMovementType type;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "medicine_id")
    private Medicine medicine;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "batch_id")
    private MedicineBatch batch;

    private int quantityChange;

    private int beforeQty;

    private int afterQty;

    @Column(length = 60)
    private String reference;

    @Column(columnDefinition = "text")
    private String reason;
}