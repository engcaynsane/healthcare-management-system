package com.hms.common;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * Base entity for records that belong to a specific branch.
 * The branch id is automatically set from the authenticated request context.
 */
@MappedSuperclass
@SuperBuilder
@Getter
@Setter
@NoArgsConstructor
public abstract class BranchScopedEntity extends BaseEntity {

    @Column(name = "branch_id")
    private Long branchId;
}