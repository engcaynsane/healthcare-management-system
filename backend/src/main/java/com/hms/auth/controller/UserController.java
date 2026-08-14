package com.hms.auth.controller;

import com.hms.auth.dto.CreateUserRequest;
import com.hms.auth.dto.UpdateUserRequest;
import com.hms.auth.dto.UserResponse;
import com.hms.auth.repository.UserRepository;
import com.hms.auth.service.UserService;
import com.hms.common.ApiResponse;
import com.hms.common.PagedResponse;
import com.hms.common.exception.ResourceNotFoundException;
import com.hms.audit.AuditService;
import com.hms.security.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;
    private final UserService userService;
    private final AuditService auditService;

    @GetMapping
    @PreAuthorize("hasAuthority('user.view')")
    public ApiResponse<PagedResponse<UserResponse>> list(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) Long branchId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Long effectiveBranch = SecurityUtils.scopedBranchId(branchId);
        var paged = userRepository.search(q, role, effectiveBranch,
                        PageRequest.of(page, size, Sort.by("id")))
                .map(userService::toResponse);
        return ApiResponse.ok(PagedResponse.of(paged));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('user.view')")
    public ApiResponse<UserResponse> get(@PathVariable Long id) {
        var user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        SecurityUtils.requireSameBranch(user.getBranchId(), "User not found");
        return ApiResponse.ok(userService.toResponse(user));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('user.create')")
    public ApiResponse<UserResponse> create(@Valid @RequestBody CreateUserRequest req) {
        UserResponse created = userService.create(req);
        auditService.log("USER_CREATE", "Created user " + created.username());
        return ApiResponse.ok(created);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('user.update')")
    public ApiResponse<UserResponse> update(@PathVariable Long id, @Valid @RequestBody UpdateUserRequest req) {
        UserResponse updated = userService.update(id, req);
        auditService.log("USER_UPDATE", "Updated user " + updated.username());
        return ApiResponse.ok(updated);
    }

    @PostMapping("/{id}/activate")
    @PreAuthorize("hasAuthority('user.update')")
    public ApiResponse<UserResponse> activate(@PathVariable Long id) {
        return ApiResponse.ok(userService.setActive(id, true));
    }

    @PostMapping("/{id}/deactivate")
    @PreAuthorize("hasAuthority('user.update')")
    public ApiResponse<UserResponse> deactivate(@PathVariable Long id) {
        return ApiResponse.ok(userService.setActive(id, false));
    }
}