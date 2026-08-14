package com.hms.auth.service;

import com.hms.auth.domain.Permission;
import com.hms.auth.domain.RefreshToken;
import com.hms.auth.domain.Role;
import com.hms.auth.domain.User;
import com.hms.auth.dto.ChangePasswordRequest;
import com.hms.auth.dto.LoginRequest;
import com.hms.auth.dto.LoginResponse;
import com.hms.auth.dto.MeResponse;
import com.hms.auth.dto.RefreshRequest;
import com.hms.auth.repository.RefreshTokenRepository;
import com.hms.auth.repository.UserRepository;
import com.hms.audit.AuditService;
import com.hms.branch.BranchRepository;
import com.hms.common.exception.BadRequestException;
import com.hms.common.exception.ResourceNotFoundException;
import com.hms.security.JwtService;
import com.hms.security.SecurityUser;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final BranchRepository branchRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;

    @Value("${app.jwt.refresh-token-days}")
    private int refreshTokenDays;

    @Transactional
    public LoginResponse login(LoginRequest request) {
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password()));
        SecurityUser principal = (SecurityUser) auth.getPrincipal();
        User user = userRepository.findById(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        String access = jwtService.generateAccessToken(user, principal.getBranchId(),
                (List) auth.getAuthorities().stream().toList());
        String refresh = newRefreshToken(user.getId());

        auditService.log("LOGIN", "User " + user.getUsername() + " logged in");
        return new LoginResponse(access, refresh, 120L * 60, toMe(user));
    }

    @Transactional
    public LoginResponse refresh(RefreshRequest request) {
        RefreshToken stored = refreshTokenRepository.findByToken(request.refreshToken())
                .orElseThrow(() -> new BadRequestException("Invalid refresh token"));
        if (stored.isRevoked() || stored.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("Refresh token expired");
        }
        User user = userRepository.findById(stored.getUserId())
                .orElseThrow(() -> new BadRequestException("Invalid refresh token"));
        if (!user.isActive()) {
            throw new BadRequestException("Account is deactivated");
        }

        stored.setRevoked(true);
        refreshTokenRepository.save(stored);

        SecurityUser principal = new SecurityUser(user);
        String access = jwtService.generateAccessToken(user, user.getBranchId(),
                (List) principal.getAuthorities().stream().toList());
        String refresh = newRefreshToken(user.getId());

        return new LoginResponse(access, refresh, 120L * 60, toMe(user));
    }

    private String newRefreshToken(Long userId) {
        String token = UUID.randomUUID().toString().replace("-", "");
        refreshTokenRepository.save(RefreshToken.builder()
                .token(token)
                .userId(userId)
                .expiresAt(LocalDateTime.now().plusDays(refreshTokenDays))
                .build());
        return token;
    }

    @Transactional
    public void logout(String refreshToken) {
        refreshTokenRepository.findByToken(refreshToken).ifPresent(rt -> {
            rt.setRevoked(true);
            refreshTokenRepository.save(rt);
        });
        auditService.log("LOGOUT", "User logged out");
    }

    public MeResponse me() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof SecurityUser su)) {
            throw new BadRequestException("Not authenticated");
        }
        User user = userRepository.findById(su.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return toMe(user);
    }

    @Transactional
    public void changePassword(ChangePasswordRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        SecurityUser su = (SecurityUser) auth.getPrincipal();
        User user = userRepository.findById(su.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (!passwordEncoder.matches(request.oldPassword(), user.getPassword())) {
            throw new BadRequestException("Current password is incorrect");
        }
        user.setPassword(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
        auditService.log("PASSWORD_CHANGE", "User " + user.getUsername() + " changed password");
    }

    @Transactional
    public LoginResponse switchBranch(Long branchId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        SecurityUser su = (SecurityUser) auth.getPrincipal();
        User user = userRepository.findById(su.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        boolean isSuperAdmin = su.getRoleCodes().contains("SUPER_ADMIN");
        boolean ownsBranch = su.getBranchId() != null && su.getBranchId().equals(branchId);
        if (!isSuperAdmin && !ownsBranch) {
            throw new BadRequestException("You are not allowed to access this branch");
        }
        branchRepository.findById(branchId)
                .orElseThrow(() -> new ResourceNotFoundException("Branch not found"));

        String access = jwtService.generateAccessToken(user, branchId,
                (List) su.getAuthorities().stream().toList());
        String refresh = newRefreshToken(user.getId());
        auditService.log("BRANCH_SWITCH", "User switched to branch " + branchId);
        return new LoginResponse(access, refresh, 120L * 60, toMe(user));
    }

    public MeResponse toMe(User user) {
        List<String> roleCodes = user.getRoles().stream().map(Role::getCode).sorted().toList();
        List<String> permissions = user.getRoles().stream()
                .flatMap(r -> r.getPermissions().stream())
                .map(Permission::getCode)
                .distinct()
                .sorted(Comparator.naturalOrder())
                .toList();
        String branchName = user.getBranchId() != null
                ? branchRepository.findById(user.getBranchId()).map(b -> b.getName()).orElse(null)
                : null;
        return new MeResponse(user.getId(), user.getUsername(), user.getFullName(), user.getEmail(),
                user.getPhone(), user.getBranchId(), branchName, roleCodes, permissions);
    }
}