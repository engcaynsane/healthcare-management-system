package com.hms.common;

/**
 * Holds the active branch id for the current request thread.
 * Populated by the JWT authentication filter from token claims.
 */
public final class BranchContext {

    private static final ThreadLocal<Long> BRANCH = new ThreadLocal<>();
    private static final ThreadLocal<Long> USER_ID = new ThreadLocal<>();
    private static final ThreadLocal<String> USERNAME = new ThreadLocal<>();

    private BranchContext() {
    }

    public static void set(Long branchId, Long userId, String username) {
        if (branchId != null) {
            BRANCH.set(branchId);
        }
        USER_ID.set(userId);
        USERNAME.set(username);
    }

    public static Long branchId() {
        return BRANCH.get();
    }

    public static Long userId() {
        return USER_ID.get();
    }

    public static String username() {
        return USERNAME.get();
    }

    public static void clear() {
        BRANCH.remove();
        USER_ID.remove();
        USERNAME.remove();
    }
}