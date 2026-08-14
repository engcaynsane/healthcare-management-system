package com.hms.notification;

import com.hms.common.ApiResponse;
import com.hms.common.BranchContext;
import com.hms.common.PagedResponse;
import com.hms.common.exception.ResourceNotFoundException;
import com.hms.security.SecurityUtils;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationRepository repository;
    private final NotificationService notificationService;

    public record NotificationResponse(Long id, String title, String message, String type,
                                       boolean read, LocalDateTime createdAt) {
        static NotificationResponse from(Notification n) {
            return new NotificationResponse(n.getId(), n.getTitle(), n.getMessage(), n.getType(),
                    n.isRead(), n.getCreatedAt());
        }
    }

    public record SendRequest(String title, String message, String type, Long userId) {
    }

    @PostMapping("/send")
    @PreAuthorize("hasAuthority('notification.send')")
    public ApiResponse<Void> send(@Valid @RequestBody SendRequest req) {
        notificationService.send(BranchContext.branchId(), req.userId(), req.title(), req.message(), req.type());
        return ApiResponse.ok("Notification sent", null);
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<PagedResponse<NotificationResponse>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        var paged = repository.findForUser(BranchContext.userId(), BranchContext.branchId(),
                PageRequest.of(page, size, Sort.by("createdAt").descending()));
        return ApiResponse.ok(PagedResponse.of(paged.map(NotificationResponse::from)));
    }

    @GetMapping("/unread-count")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<Map<String, Long>> unreadCount() {
        return ApiResponse.ok(Map.of("count",
                repository.countUnreadForUser(BranchContext.userId(), BranchContext.branchId())));
    }

    @PostMapping("/{id}/read")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<NotificationResponse> markRead(@PathVariable Long id) {
        Notification n = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found"));
        if (n.getUserId() != null && !n.getUserId().equals(BranchContext.userId())) {
            throw new ResourceNotFoundException("Notification not found");
        }
        SecurityUtils.requireSameBranch(n.getBranchId(), "Notification not found");
        n.setRead(true);
        n.setReadAt(LocalDateTime.now());
        return ApiResponse.ok(NotificationResponse.from(repository.save(n)));
    }

    @PostMapping("/read-all")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<Void> markAllRead() {
        repository.markAllRead(BranchContext.userId(), BranchContext.branchId(), LocalDateTime.now());
        return ApiResponse.ok("All notifications marked read", null);
    }
}