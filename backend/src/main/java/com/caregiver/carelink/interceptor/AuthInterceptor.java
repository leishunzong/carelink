package com.caregiver.carelink.interceptor;

import com.caregiver.carelink.common.context.AdminContextHolder;
import com.caregiver.carelink.common.context.CaregiverContextHolder;
import com.caregiver.carelink.common.context.UserContextHolder;
import com.caregiver.carelink.common.result.Result;
import com.caregiver.carelink.common.result.ResultCode;
import com.caregiver.carelink.utils.JwtUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 登录认证拦截器（基于路径自动判断）
 *
 * @author CareLink
 * @since 2026-01-29
 */
@Component
public class AuthInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(AuthInterceptor.class);
    @Resource
    private JwtUtils jwtUtils;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String requestPath = request.getRequestURI();

        // 获取Token
        String token = request.getHeader("Authorization");
        if (token == null || token.isEmpty()) {
            token = request.getHeader("token");
        }

        // Token为空，返回未授权
        if (token == null || token.isEmpty()) {
            responseUnauthorized(response, "请先登录");
            return false;
        }

        // 移除 "Bearer " 前缀
        if (token.startsWith("Bearer ")) {
            token = token.substring(7);
        }

        // 验证Token
        if (!jwtUtils.validateToken(token) || jwtUtils.isTokenExpired(token)) {
            log.info("token: {}", token);
            responseUnauthorized(response, "Token无效或已过期");
            return false;
        }

        // 解析Token，获取用户信息
        Claims claims = jwtUtils.getClaimsFromToken(token);
        if (claims == null) {
            responseUnauthorized(response, "Token解析失败");
            return false;
        }

        String userType = (String) claims.get("userType");

        if ("admin".equals(userType)) {
            Long adminId = jwtUtils.getAdminIdFromToken(token);
            AdminContextHolder.setAdminId(adminId);
            return true;
        }
        if (requestPath.startsWith("/api/admin")) {
            responseForbidden(response, "请使用管理员账号登录");
            return false;
        }
        if ("user".equals(userType)) {
            Long userId = jwtUtils.getUserIdFromToken(token);
            UserContextHolder.setUserId(userId);
        } else if ("caregiver".equals(userType)) {
            Long caregiverId = jwtUtils.getCaregiverIdFromToken(token);
            CaregiverContextHolder.setCaregiverId(caregiverId);
        } else {
            responseForbidden(response, "非法访问");
            return false;
        }

        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        UserContextHolder.clear();
        CaregiverContextHolder.clear();
        AdminContextHolder.clear();
    }

    /**
     * 返回未授权响应
     */
    private void responseUnauthorized(HttpServletResponse response, String message) throws IOException {
        response.setContentType("application/json;charset=UTF-8");
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        Result<Void> result = Result.fail(ResultCode.UNAUTHORIZED.getCode(), message);
        response.getWriter().write(new ObjectMapper().writeValueAsString(result));
    }

    /**
     * 返回无权限响应
     */
    private void responseForbidden(HttpServletResponse response, String message) throws IOException {
        response.setContentType("application/json;charset=UTF-8");
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        Result<Void> result = Result.fail(ResultCode.FORBIDDEN.getCode(), message);
        response.getWriter().write(new ObjectMapper().writeValueAsString(result));
    }
}

