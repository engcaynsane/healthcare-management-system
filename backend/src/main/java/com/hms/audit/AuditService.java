package com.hms.audit;

import com.hms.common.BranchContext;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository repository;

    public void log(String action, String details) {
        try {
            AuditLog entry = AuditLog.builder()
                    .action(action)
                    .details(details != null && details.length() > 512 ? details.substring(0, 512) : details)
                    .userId(BranchContext.userId())
                    .username(BranchContext.username())
                    .ipAddress(currentIp())
                    .build();
            entry.setBranchId(BranchContext.branchId());
            repository.save(entry);
        } catch (Exception ignored) {
            // auditing must never break the business flow
        }
    }

    private String currentIp() {
        try {
            ServletRequestAttributes attrs =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                HttpServletRequest req = attrs.getRequest();
                String xff = req.getHeader("X-Forwarded-For");
                if (xff != null && !xff.isBlank()) {
                    return xff.split(",")[0].trim();
                }
                return req.getRemoteAddr();
            }
        } catch (Exception ignored) {
        }
        return null;
    }
}