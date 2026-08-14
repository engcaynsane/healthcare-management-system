package com.hms.pharmacy;

import com.hms.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "medicine_categories")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class MedicineCategory extends BaseEntity {

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @Column(length = 255)
    private String description;
}