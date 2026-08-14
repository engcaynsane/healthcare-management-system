package com.hms.inventory;

import com.hms.common.BranchScopedEntity;
import com.hms.pharmacy.Medicine;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "inventory", uniqueConstraints = {
        @UniqueConstraint(name = "uk_inventory_branch_med_batch", columnNames = {"branch_id", "medicine_id", "batch_id"})
})
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class Inventory extends BranchScopedEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "medicine_id")
    private Medicine medicine;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "batch_id")
    private MedicineBatch batch;

    @Builder.Default
    private int quantity = 0;

    @Column(length = 50)
    private String location;
}