package com.hms.doctor;

import com.hms.auth.domain.User;
import com.hms.common.BranchScopedEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

@Entity
@Table(name = "doctors")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class Doctor extends BranchScopedEntity {

@ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false, length = 80)
    private String firstName;

    @Column(nullable = false, length = 80)
    private String lastName;

    @Column(length = 100)
    private String specialty;

    @Column(length = 40)
    private String licenseNumber;

    private BigDecimal consultationFee;

    @Builder.Default
    private boolean active = true;
}