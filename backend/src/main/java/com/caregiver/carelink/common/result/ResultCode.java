package com.caregiver.carelink.common.result;

/**
 * 响应状态码枚举
 *
 * @author CareLink
 * @since 2026-01-29
 */
public enum ResultCode {

    /**
     * 成功
     */
    SUCCESS(200, "操作成功"),

    /**
     * 失败
     */
    FAIL(500, "操作失败"),

    /**
     * 参数错误
     */
    PARAM_ERROR(400, "参数错误"),

    /**
     * 参数为空
     */
    PARAM_IS_NULL(400, "参数为空"),

    /**
     * 未授权
     */
    UNAUTHORIZED(401, "未授权，请先登录"),

    /**
     * 无权限
     */
    FORBIDDEN(403, "无权限访问"),

    /**
     * 资源不存在
     */
    NOT_FOUND(404, "资源不存在"),

    /**
     * 请求方法不支持
     */
    METHOD_NOT_ALLOWED(405, "请求方法不支持"),

    /**
     * 服务器内部错误
     */
    INTERNAL_SERVER_ERROR(500, "服务器内部错误"),

    /**
     * 业务异常
     */
    BUSINESS_ERROR(500, "业务处理失败"),

    /**
     * 数据库操作失败
     */
    DATABASE_ERROR(500, "数据库操作失败"),

    /**
     * Token过期
     */
    TOKEN_EXPIRED(401, "Token已过期"),

    /**
     * Token无效
     */
    TOKEN_INVALID(401, "Token无效");

    /**
     * 状态码
     */
    private final Integer code;

    /**
     * 消息
     */
    private final String message;

    /**
     * 构造方法
     */
    ResultCode(Integer code, String message) {
        this.code = code;
        this.message = message;
    }

    /**
     * 获取状态码
     */
    public Integer getCode() {
        return code;
    }

    /**
     * 获取消息
     */
    public String getMessage() {
        return message;
    }
}
