package com.caregiver.carelink.common.constant;

/**
 * Redis键常量类
 *
 * @author CareLink
 * @since 2026-01-29
 */
public class RedisKeyConstants {

    /**
     * 验证码Redis键前缀
     */
    public static final String CAPTCHA_CODE_KEY = "captcha:code:";

    /**
     * 登录用户Redis键前缀
     */
    public static final String LOGIN_TOKEN_KEY = "login:token:";

    /**
     * 用户信息Redis键前缀
     */
    public static final String USER_INFO_KEY = "user:info:";

    /**
     * 防重提交Redis键前缀
     */
    public static final String REPEAT_SUBMIT_KEY = "repeat:submit:";

    /**
     * 限流Redis键前缀
     */
    public static final String RATE_LIMIT_KEY = "rate:limit:";

    /**
     * 缓存Redis键前缀
     */
    public static final String CACHE_KEY = "cache:";

    /**
     * 接单中的护工位置GEO键前缀
     */
    public static final String CAREGIVER_LOCATION_GEO_PREFIX = "caregiver:locations:available:";

    /**
     * 生成验证码Redis键
     */
    public static String getCaptchaKey(String uuid) {
        return CAPTCHA_CODE_KEY + uuid;
    }

    /**
     * 生成登录用户Token Redis键
     */
    public static String getTokenKey(String token) {
        return LOGIN_TOKEN_KEY + token;
    }

    /**
     * 生成用户信息Redis键
     */
    public static String getUserInfoKey(Long userId) {
        return USER_INFO_KEY + userId;
    }

    /**
     * 生成护工位置GEO键（按城市分区）
     */
    public static String getCaregiverLocationGeoKey(String cityCode) {
        return CAREGIVER_LOCATION_GEO_PREFIX + cityCode;
    }

    /**
     * 生成护工在GEO中的member名称（直接用护工ID）
     */
    public static String getCaregiverGeoMember(Long caregiverId) {
        return String.valueOf(caregiverId);
    }

    /**
     * 服务包可接单护工集合键前缀（Set 存储该服务包下可提供服务的护工 ID）
     * 用于系统匹配时快速查询某服务包下有哪些护工可接单
     */
    public static final String PACKAGE_CAREGIVERS_SET_PREFIX = "package:caregivers:";

    /**
     * 生成某服务包对应的护工 ID 集合 Redis 键
     */
    public static String getPackageCaregiversKey(Long packageId) {
        return PACKAGE_CAREGIVERS_SET_PREFIX + packageId;
    }
}
