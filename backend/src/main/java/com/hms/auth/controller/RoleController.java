package com.hms.auth.controller;

import com.hms.auth.domain.Permission;
import com.hms.auth.domain.Role;
import com.hms.auth.repository.PermissionRepository;
import com.hms.auth.repository.RoleRepository;
import com.hms.common.ApiResponse;
import com.hms.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/roles")
@RequiredArgsConstructor
public class RoleController {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;

    public record RoleResponse(Long id, String code, String name, String description,
                               List<String> permissions) {
        static RoleResponse from(Role r) {
            return new RoleResponse(r.getId(), r.getCode(), r.getName(), r.getDescription(),
                    r.getPermissions().stream().map(Permission::getCode).sorted().toList());
        }
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('user.view','role.manage')")
    public ApiResponse<List<RoleResponse>> list() {
        return ApiResponse.ok(roleRepository.findAll().stream().map(RoleResponse::from).toList());
    }

    @GetMapping("/permissions")
    @PreAuthorize("hasAuthority('role.manage')")
    public ApiResponse<List<String>> permissions() {
        return ApiResponse.ok(permissionRepository.findAll().stream()
                .map(Permission::getCode).sorted().toList());
    }

    @PutMapping("/{id}/permissions")
    @PreAuthorize("hasAuthority('role.manage')")
    public ApiResponse<RoleResponse> updatePermissions(@PathVariable Long id,
                                                       @RequestBody List<String> permissionCodes) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found"));
        Set<Permission> permissions = new HashSet<>();
        for (String code : permissionCodes) {
            permissionRepository.findByCode(code).ifPresent(permissions::add);
        }
        role.setPermissions(permissions);
        return ApiResponse.ok(RoleResponse.from(roleRepository.save(role)));
    }
}