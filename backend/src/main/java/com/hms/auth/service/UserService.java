package com.hms.auth.service;

import com.hms.auth.domain.Role;
import com.hms.auth.domain.User;
import com.hms.auth.dto.CreateUserRequest;
import com.hms.auth.dto.UpdateUserRequest;
import com.hms.auth.dto.UserResponse;
import com.hms.auth.repository.RoleRepository;
import com.hms.auth.repository.UserRepository;
import com.hms.branch.BranchRepository;
import com.hms.common.BranchContext;
import com.hms.common.exception.BadRequestException;
import com.hms.common.exception.DuplicateResourceException;
import com.hms.common.exception.ResourceNotFoundException;
import com.hms.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final BranchRepository branchRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public UserResponse create(CreateUserRequest req) {
        if (userRepository.existsByUsername(req.username())) {
            throw new DuplicateResourceException("Username already exists");
        }
        Set<Role> roles = resolveRoles(req.roleCodes());
        Long branchId = SecurityUtils.isSuperAdmin()
                ? req.branchId()
                : BranchContext.branchId();
        validateBranch(branchId);

        User user = User.builder()
                .username(req.username())
                .password(passwordEncoder.encode(req.password()))
                .fullName(req.fullName())
                .email(req.email())
                .phone(req.phone())
                .active(true)
                .branchId(branchId)
                .roles(roles)
                .build();
        return toResponse(userRepository.save(user));
    }

    @Transactional
    public UserResponse update(Long id, UpdateUserRequest req) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        SecurityUtils.requireSameBranch(user.getBranchId(), "User not found");
        user.setFullName(req.fullName());
        user.setEmail(req.email());
        user.setPhone(req.phone());
        if (req.active() != null) {
            user.setActive(req.active());
        }
        if (req.roleCodes() != null) {
            user.setRoles(resolveRoles(req.roleCodes()));
        }
        if (req.branchId() != null) {
            if (!SecurityUtils.isSuperAdmin()) {
                throw new BadRequestException("You cannot move a user to another branch");
            }
            validateBranch(req.branchId());
            user.setBranchId(req.branchId());
        }
        return toResponse(userRepository.save(user));
    }

    @Transactional
    public UserResponse setActive(Long id, boolean active) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        SecurityUtils.requireSameBranch(user.getBranchId(), "User not found");
        user.setActive(active);
        return toResponse(userRepository.save(user));
    }

    private Set<Role> resolveRoles(java.util.List<String> roleCodes) {
        Set<Role> roles = new HashSet<>();
        if (roleCodes != null) {
            for (String code : roleCodes) {
                roles.add(roleRepository.findByCode(code)
                        .orElseThrow(() -> new BadRequestException("Role not found: " + code)));
            }
        }
        return roles;
    }

    private void validateBranch(Long branchId) {
        if (branchId != null && !branchRepository.existsById(branchId)) {
            throw new BadRequestException("Branch not found: " + branchId);
        }
    }

    public UserResponse toResponse(User user) {
        String branchName = user.getBranchId() != null
                ? branchRepository.findById(user.getBranchId()).map(b -> b.getName()).orElse(null)
                : null;
        return new UserResponse(user.getId(), user.getUsername(), user.getFullName(), user.getEmail(),
                user.getPhone(), user.isActive(), user.getBranchId(), branchName,
                user.getRoles().stream().map(Role::getCode).sorted().toList());
    }
}