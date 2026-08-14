package com.hms.supplier;

import com.hms.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "suppliers")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class Supplier extends BaseEntity {

    @Column(nullable = false, unique = true, length = 120)
    private String name;

    @Column(length = 80)
    private String contactPerson;

    @Column(length = 32)
    private String phone;

    @Column(length = 128)
    private String email;

    @Column(length = 255)
    private String address;

    @Builder.Default
    private boolean active = true;
}