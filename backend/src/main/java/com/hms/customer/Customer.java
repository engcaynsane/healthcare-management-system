package com.hms.customer;

import com.hms.common.BranchScopedEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

@Entity
@Table(name = "customers", indexes = @Index(name = "idx_customer_branch", columnList = "branch_id"))
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class Customer extends BranchScopedEntity {

    @Column(nullable = false, length = 120)
    private String name;

    @Column(length = 32)
    private String phone;

    @Column(length = 128)
    private String email;

    @Column(length = 255)
    private String address;

    @Builder.Default
    private int loyaltyPoints = 0;

    private BigDecimal creditLimit;

    @Builder.Default
    private BigDecimal balance = BigDecimal.ZERO;

    @Column(columnDefinition = "text")
    private String notes;

    @Builder.Default
    private boolean active = true;
}