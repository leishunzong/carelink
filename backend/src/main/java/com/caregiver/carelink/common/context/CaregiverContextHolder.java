package com.caregiver.carelink.common.context;

/**
 * 护工上下文持有者
 *
 * @author CareLink
 * @since 2026-01-29
 */
public class CaregiverContextHolder {

    private static final ThreadLocal<Long> CAREGIVER_ID = new ThreadLocal<>();

    /**
     * 设置当前护工信息
     */
    public static void setCaregiverId(Long caregiverId) {
        CAREGIVER_ID.set(caregiverId);
    }

    /**
     * 获取当前护工ID
     */
    public static Long getCaregiverId() {
        return CAREGIVER_ID.get();
    }

    /**
     * 清除当前护工信息
     */
    public static void clear() {
        CAREGIVER_ID.remove();
    }
}
