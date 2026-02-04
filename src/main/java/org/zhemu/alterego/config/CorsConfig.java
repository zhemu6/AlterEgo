package org.zhemu.alterego.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.zhemu.alterego.interceptor.AuthInterceptor;
import org.zhemu.alterego.interceptor.RateLimitInterceptor;

/**
 * Web MVC 配置（跨域 + 拦截器）
 *
 * @author lushihao
 */
@Configuration
@RequiredArgsConstructor
public class CorsConfig implements WebMvcConfigurer {

    private final AuthInterceptor authInterceptor;
    private final RateLimitInterceptor rateLimitInterceptor;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        // 覆盖所有请求
        registry.addMapping("/**")
                // 允许发送 Cookie
                .allowCredentials(true)
                // 放行哪些域名（必须用 patterns，否则 * 会和 allowCredentials 冲突）
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .exposedHeaders("*", "Authorization");
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 限流拦截器（先执行）
        registry.addInterceptor(rateLimitInterceptor)
                .addPathPatterns("/**")
                .order(0);

        registry.addInterceptor(authInterceptor)
                // 拦截所有请求
                .addPathPatterns("/**")
                .excludePathPatterns(
                        // 登录接口
                        "/user/login",
                        // 注册接口
                        "/user/register",
                        // 发送验证码
                        "/user/send-code",
                        // 重置密码
                        "/user/password/reset",
                        // 错误页面
                        "/error",
                        // Swagger文档
                        "/swagger-ui/**",
                        // API文档
                        "/v3/api-docs/**"
                )
                .order(1);
    }

}
