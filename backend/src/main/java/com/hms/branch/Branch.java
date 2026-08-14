package com.hms.branch;

import com.hms.common.BaseEntity;
import jakarta.persistence.*;
import lombok.experimental.SuperBuilder;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "branches")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class Branch extends BaseEntity {

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @Column(nullable = false, unique = true, length = 20)
    private String code;

    private String address;

    @Column(length = 32)
    private String phone;

    @Column(length = 128)
    private String email;

    @Builder.Default
    private boolean active = true;

    @Builder.Default
    private boolean central = false;
}