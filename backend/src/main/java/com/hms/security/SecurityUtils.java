package com.hms.security;

import com.hms.common.BranchContext;
import com.hms.common.exception.ResourceNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Helpers for the current request's security context.
 * <p>
 * Branch scoping is enforced against the active branch stored in the JWT
 * ({@link BranchContext}). SUPER_ADMIN is exempt so the global administrator
 * can operate across branches, but every other user is limited to their own
 * branch.
 */
public final class SecurityUtils {

    private SecurityUtils() {
    }

    public static boolean isSuperAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof SecurityUser su)) {
            return false;
        }
        return su.getRoleCodes().contains("SUPER_ADMIN");
    }

    /**
     * The branch the current user may operate on. For a non-super-admin this is
     * always their own branch (never a client-supplied value). A sentinel value is
     * returned for branchless non-super-admins so branch-scoped queries match
     * nothing instead of leaking every branch.
     */
    public static Long scopedBranchId(Long requested) {
        if (isSuperAdmin()) {
            return requested;
        }
        Long current = BranchContext.branchId();
        return current != null ? current : Long.MIN_VALUE;
    }

    /**
     * Throws {@link ResourceNotFoundException} when the current user (unless
     * SUPER_ADMIN) is not acting in the branch the record belongs to.
     */
    public static void requireSameBranch(Long entityBranchId, String message) {
        if (isSuperAdmin()) {
            return;
        }
        Long current = BranchContext.branchId();
        if (entityBranchId == null || current == null || !entityBranchId.equals(current)) {
            throw new ResourceNotFoundException(message);
        }
    }
}
