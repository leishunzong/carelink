package com.caregiver.carelink.config;

import com.caregiver.carelink.interceptor.AuthInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.annotation.Resource;

/**
 * Web MVC配置类
 *
 * @author CareLink
 * @since 2026-01-29
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Resource
    private AuthInterceptor authInterceptor;

    /**
     * 跨域配置
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }

    /**
     * 注册拦截器
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(
                        // 用户端公开接口
                        "/user/login",
                        "/user/register",
                        // 护工端公开接口
                        "/caregiver/login",
                        "/caregiver/register",
                        // 管理员登录
                        "/admin/login",
                        // 健康检查
                        "/health",
                        // Swagger文档
                        "/doc.html",
                        "/swagger-resources/**",
                        "/webjars/**",
                        "/v2/**",
                        "/v3/**",
                        "/swagger-ui.html",
                        "/swagger-ui/**",
                        // Druid监控
                        "/druid/**",
                        // 静态资源
                        "/static/**",
                        "/error"
                );
    }
}
