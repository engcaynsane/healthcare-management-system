package com.hms.notification;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationRepository repository;

    /**
     * Target a notification at a specific user. Visible only to that user.
     */
    @Transactional
    public void notifyUser(Long branchId, Long userId, String title, String message, String type) {
        try {
            repository.save(Notification.builder()
                    .branchId(branchId)
                    .userId(userId)
                    .title(title)
                    .message(message)
                    .type(type)
                    .read(false)
                    .build());
        } catch (Exception ex) {
            log.warn("Failed to create notification for user {}: {}", userId, ex.getMessage());
        }
    }

    /**
     * Branch-wide notification (userId null). Visible to every user in the branch.
     */
    @Transactional
    public void notifyBranch(Long branchId, String title, String message, String type) {
        try {
            repository.save(Notification.builder()
                    .branchId(branchId)
                    .title(title)
                    .message(message)
                    .type(type)
                    .read(false)
                    .build());
        } catch (Exception ex) {
            log.warn("Failed to create branch notification {}: {}", branchId, ex.getMessage());
        }
    }

    /**
     * Send a notification. When {@code userId} is null the notification is branch-wide.
     */
    @Transactional
    public void send(Long branchId, Long userId, String title, String message, String type) {
        if (userId != null) {
            notifyUser(branchId, userId, title, message, type);
        } else {
            notifyBranch(branchId, title, message, type);
        }
    }
}