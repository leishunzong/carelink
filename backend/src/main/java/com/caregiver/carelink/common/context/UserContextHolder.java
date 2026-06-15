package com.caregiver.carelink.common.context;

/**
 * 普通用户上下文持有者
 *
 * @author CareLink
 * @since 2026-01-29
 */
public class UserContextHolder {

    private static final ThreadLocal<Long> USER_ID = new ThreadLocal<>();

    /**
     * 设置当前用户信息
     */
    public static void setUserId(Long userId) {
        USER_ID.set(userId);
    }

    /**
     * 获取当前用户ID
     */
    public static Long getUserId() {
        return USER_ID.get();
    }


    /**
     * 清除当前用户信息
     */
    public static void clear() {
        USER_ID.remove();
    }
}
