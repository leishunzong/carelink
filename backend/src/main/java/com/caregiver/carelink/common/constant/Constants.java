package com.caregiver.carelink.common.constant;

/**
 * 通用常量类
 *
 * @author CareLink
 * @since 2026-01-29
 */
public class Constants {

    /**
     * UTF-8编码
     */
    public static final String UTF8 = "UTF-8";

    /**
     * 成功标记
     */
    public static final Integer SUCCESS = 200;

    /**
     * 失败标记
     */
    public static final Integer FAIL = 500;

    /**
     * 登录成功状态
     */
    public static final String LOGIN_SUCCESS_STATUS = "0";

    /**
     * 登录失败状态
     */
    public static final String LOGIN_FAIL_STATUS = "1";

    /**
     * 验证码有效期（分钟）
     */
    public static final Integer CAPTCHA_EXPIRATION = 2;

    /**
     * Token前缀
     */
    public static final String TOKEN_PREFIX = "Bearer ";

    /**
     * Token请求头
     */
    public static final String TOKEN_HEADER = "Authorization";

    /**
     * 用户ID
     */
    public static final String USER_ID = "userId";

    /**
     * 用户名
     */
    public static final String USER_NAME = "username";

    /**
     * 管理员角色权限标识
     */
    public static final String ROLE_ADMIN = "admin";

    /**
     * 普通用户角色权限标识
     */
    public static final String ROLE_USER = "user";
}
