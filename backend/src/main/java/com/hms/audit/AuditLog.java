package com.hms.audit;

import com.hms.common.BranchScopedEntity;
import lombok.experimental.SuperBuilder;
import jakarta.persistence.Column;
import lombok.experimental.SuperBuilder;
import jakarta.persistence.Entity;
import lombok.experimental.SuperBuilder;
import jakarta.persistence.Index;
import lombok.experimental.SuperBuilder;
import jakarta.persistence.Table;
import lombok.experimental.SuperBuilder;
import lombok.AllArgsConstructor;
import lombok.experimental.SuperBuilder;
import lombok.Builder;
import lombok.experimental.SuperBuilder;
import lombok.Getter;
import lombok.experimental.SuperBuilder;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "audit_logs", indexes = {
        @Index(name = "idx_audit_branch", columnList = "branch_id"),
        @Index(name = "idx_audit_user", columnList = "userId")
})
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class AuditLog extends BranchScopedEntity {

    @Column(nullable = false, length = 64)
    private String action;

    @Column(length = 512)
    private String details;

    private Long userId;

    @Column(length = 64)
    private String username;

    @Column(length = 45)
    private String ipAddress;
}