package com.hms.inventory;

import com.hms.common.BranchScopedEntity;
import com.hms.pharmacy.Medicine;
import com.hms.supplier.Supplier;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "medicine_batches", indexes = {
        @Index(name = "idx_batch_branch", columnList = "branch_id"),
        @Index(name = "idx_batch_expiry", columnList = "expiryDate")
})
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class MedicineBatch extends BranchScopedEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "medicine_id")
    private Medicine medicine;

    @Column(nullable = false, length = 50)
    private String batchNo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supplier_id")
    private Supplier supplier;

    @Column(nullable = false)
    private LocalDate expiryDate;

    private LocalDate purchaseDate;

    private BigDecimal costPrice;

    private int initialQuantity;

    @Builder.Default
    private int quantity = 0;
}