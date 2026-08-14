package com.hms.lab;

import com.hms.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

@Entity
@Table(name = "lab_tests")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class LabTest extends BaseEntity {

    @Column(nullable = false, unique = true, length = 40)
    private String code;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(length = 80)
    private String category;

    @Column(precision = 14, scale = 2)
    private BigDecimal price;

    @Column(length = 255)
    private String description;

    @Builder.Default
    private boolean active = true;
}