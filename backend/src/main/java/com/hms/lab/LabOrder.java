package com.hms.lab;

import com.hms.common.BranchScopedEntity;
import com.hms.common.enums.LabOrderStatus;
import com.hms.patient.Patient;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

@Entity
@Table(name = "lab_orders", indexes = {
        @Index(name = "idx_lab_branch_status", columnList = "branch_id,status")
})
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class LabOrder extends BranchScopedEntity {

    @Column(nullable = false, unique = true, length = 40)
    private String orderNumber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "patient_id")
    private Patient patient;

    private Long requestedByUserId;

    @Column(length = 64)
    private String requestedByName;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    @Builder.Default
    private LabOrderStatus status = LabOrderStatus.REQUESTED;

    private Long resultApprovedByUserId;

    private LocalDateTime completedAt;
}