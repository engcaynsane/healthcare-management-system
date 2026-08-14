package com.hms.notification;

import com.hms.common.BranchScopedEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

@Entity
@Table(name = "notifications", indexes = @Index(name = "idx_notif_user", columnList = "userId"))
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class Notification extends BranchScopedEntity {

    @Column(nullable = false, length = 150)
    private String title;

    @Column(columnDefinition = "text")
    private String message;

    @Column(length = 40)
    private String type;

    private Long userId;

    @Builder.Default
    private boolean read = false;

    private LocalDateTime readAt;
}