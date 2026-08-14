package com.hms.audit;

import com.hms.common.ApiResponse;
import com.hms.common.BranchContext;
import com.hms.common.PagedResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/audit-logs")
@RequiredArgsConstructor
public class AuditController {

    private final AuditLogRepository repository;

    @GetMapping
    @PreAuthorize("hasAuthority('audit.view')")
    public ApiResponse<PagedResponse<AuditLog>> list(
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String username,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        var paged = repository.search(BranchContext.branchId(), action, username,
                PageRequest.of(page, size, Sort.by("createdAt").descending()));
        return ApiResponse.ok(PagedResponse.of(paged));
    }
}