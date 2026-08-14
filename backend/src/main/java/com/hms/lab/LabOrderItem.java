package com.hms.lab;

import com.hms.common.BranchScopedEntity;
import com.hms.common.enums.LabOrderStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

@Entity
@Table(name = "lab_order_items")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class LabOrderItem extends BranchScopedEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lab_order_id")
    private LabOrder labOrder;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lab_test_id")
    private LabTest labTest;

    @Column(precision = 14, scale = 2)
    private BigDecimal price;

    @Column(columnDefinition = "text")
    private String result;

    @Column(columnDefinition = "text")
    private String resultNotes;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    @Builder.Default
    private LabOrderStatus status = LabOrderStatus.REQUESTED;
}