package com.hms.pharmacy;

import com.hms.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

@Entity
@Table(name = "medicines", indexes = @Index(name = "idx_med_barcode", columnList = "barcode"))
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class Medicine extends BaseEntity {

    @Column(nullable = false, length = 150)
    private String name;

    @Column(length = 150)
    private String genericName;

    @Column(length = 100)
    private String brand;

@ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "category_id")
    private MedicineCategory category;

    @Column(length = 50)
    private String strength;

    @Column(length = 50)
    private String dosageForm;

    @Column(nullable = false, unique = true, length = 64)
    private String barcode;

    @Column(length = 20)
    private String packSize;

    @Column(length = 20)
    private String unit;

    private int reorderLevel;

    @Builder.Default
    private boolean requirePrescription = false;

    private BigDecimal sellingPrice;

    private BigDecimal costPrice;

    @Builder.Default
    private boolean active = true;
}