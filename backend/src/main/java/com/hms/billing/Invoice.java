package com.hms.billing;

import com.hms.common.BranchScopedEntity;
import com.hms.common.enums.PaymentStatus;
import com.hms.customer.Customer;
import com.hms.patient.Patient;
import com.hms.sale.Sale;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

@Entity
@Table(name = "invoices", indexes = @Index(name = "idx_inv_branch", columnList = "branch_id"))
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class Invoice extends BranchScopedEntity {

    @Column(nullable = false, unique = true, length = 40)
    private String invoiceNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id")
    private Patient patient;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sale_id")
    private Sale sale;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lab_order_id")
    private com.hms.lab.LabOrder labOrder;

    @Column(length = 255)
    private String description;

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

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    @Builder.Default
    private PaymentStatus status = PaymentStatus.UNPAID;

    private Long issuedByUserId;

    @Column(length = 64)
    private String issuedByName;
}