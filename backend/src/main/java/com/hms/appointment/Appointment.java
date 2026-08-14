package com.hms.appointment;

import com.hms.common.BranchScopedEntity;
import com.hms.common.enums.AppointmentStatus;
import com.hms.doctor.Doctor;
import com.hms.patient.Patient;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

@Entity
@Table(name = "appointments", indexes = {
        @Index(name = "idx_appt_branch_date", columnList = "branch_id,startTime")
})
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class Appointment extends BranchScopedEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "doctor_id", nullable = false)
    private Doctor doctor;

    @Column(nullable = false)
    private LocalDateTime startTime;

    private LocalDateTime endTime;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    @Builder.Default
    private AppointmentStatus status = AppointmentStatus.SCHEDULED;

    @Column(length = 200)
    private String purpose;

    @Column(columnDefinition = "text")
    private String notes;
}