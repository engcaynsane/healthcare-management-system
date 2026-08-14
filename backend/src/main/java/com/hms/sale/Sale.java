package com.hms.sale;

import com.hms.common.BranchScopedEntity;
import com.hms.common.enums.PaymentMethod;
import com.hms.common.enums.SaleStatus;
import com.hms.customer.Customer;
import com.hms.patient.Patient;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

@Entity
@Table(name = "sales", indexes = {
        @Index(name = "idx_sale_branch_date", columnList = "branch_id,createdAt")
})
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class Sale extends BranchScopedEntity {

    @Column(nullable = false, unique = true, length = 40)
    private String saleNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id")
    private Patient patient;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal subtotal;

    @Builder.Default
    @Column(precision = 14, scale = 2)
    private BigDecimal discount = BigDecimal.ZERO;

    @Builder.Default
    @Column(precision = 14, scale = 2)
    private BigDecimal tax = BigDecimal.ZERO;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal total;

    @Builder.Default
    @Column(precision = 14, scale = 2)
    private BigDecimal paidAmount = BigDecimal.ZERO;

    @Builder.Default
    @Column(precision = 14, scale = 2)
    private BigDecimal changeAmount = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private PaymentMethod paymentMethod;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    @Builder.Default
    private SaleStatus status = SaleStatus.COMPLETED;

    private Long cashierUserId;

    @Column(length = 64)
    private String cashierName;

    @Column(columnDefinition = "text")
    private String note;
}