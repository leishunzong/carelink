package com.caregiver.carelink.common.context;

/**
 * 管理员上下文持有者
 *
 * @author CareLink
 * @since 2026-02-24
 */
public class AdminContextHolder {

    private static final ThreadLocal<Long> ADMIN_ID = new ThreadLocal<>();

    public static void setAdminId(Long adminId) {
        ADMIN_ID.set(adminId);
    }

    public static Long getAdminId() {
        return ADMIN_ID.get();
    }

    public static void clear() {
        ADMIN_ID.remove();
    }
}
