package com.hms.auth.domain;

import com.hms.common.BaseEntity;
import jakarta.persistence.*;
import lombok.experimental.SuperBuilder;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "refresh_tokens", indexes = @Index(name = "idx_refresh_user", columnList = "userId"))
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class RefreshToken extends BaseEntity {

@Column(nullable = false, unique = true, length = 1024)
    private String token;

    private Long userId;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    @Builder.Default
    private boolean revoked = false;
}