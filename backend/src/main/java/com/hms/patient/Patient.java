package com.hms.patient;

import com.hms.common.BranchScopedEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;

@Entity
@Table(name = "patients", indexes = {
        @Index(name = "idx_patients_branch", columnList = "branch_id"),
        @Index(name = "idx_patients_code", columnList = "patientCode")
})
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class Patient extends BranchScopedEntity {

    @Column(nullable = false, unique = true, length = 30)
    private String patientCode;

    @Column(nullable = false, length = 80)
    private String firstName;

    @Column(nullable = false, length = 80)
    private String lastName;

    @Column(length = 10)
    private String gender;

    private LocalDate dateOfBirth;

    @Column(length = 32)
    private String phone;

    @Column(length = 128)
    private String email;

    @Column(length = 20)
    private String bloodGroup;

    @Column(length = 40)
    private String nationalId;

    @Column(columnDefinition = "text")
    private String allergies;

    @Column(columnDefinition = "text")
    private String medicalHistory;

    @Column(length = 80)
    private String emergencyContactName;

    @Column(length = 32)
    private String emergencyContactPhone;

    @Builder.Default
    private boolean active = true;
}