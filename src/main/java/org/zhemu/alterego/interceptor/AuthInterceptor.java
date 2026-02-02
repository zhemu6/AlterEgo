package org.zhemu.alterego.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.zhemu.alterego.annotation.RequireLogin;
import org.zhemu.alterego.annotation.RequireRole;
import org.zhemu.alterego.constant.UserRole;
import org.zhemu.alterego.exception.BusinessException;
import org.zhemu.alterego.exception.ErrorCode;
import org.zhemu.alterego.model.entity.SysUser;
import org.zhemu.alterego.service.SysUserService;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

/**
 * 认证和权限拦截器
 * @author lushihao
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuthInterceptor implements HandlerInterceptor {

    private final StringRedisTemplate stringRedisTemplate;
    private final SysUserService sysUserService;
    
    private static final String TOKEN_HEADER = "Authorization";
    private static final String TOKEN_PREFIX = "Bearer ";
    private static final String REDIS_LOGIN_KEY = "user:login:";

    @Override
    public boolean preHandle(@NotNull HttpServletRequest request,
                             @NotNull HttpServletResponse response,
                             Object handler) throws Exception {
        
        // 1. 不是Controller方法直接放行
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }

        // 2. 检查是否需要登录
        boolean needLogin = checkNeedLogin(handlerMethod);
        
        if (!needLogin) {
            // 不需要登录，直接放行
            return true;
        }

        // 3. 验证登录并获取用户信息
        SysUser currentUser = validateLoginAndGetUser(request);
        
        // 4. 将用户信息放入request attribute
        request.setAttribute("currentUser", currentUser);
        request.setAttribute("userId", currentUser.getId());
        request.setAttribute("userRole", currentUser.getUserRole());

        // 5. 检查角色权限
        checkRolePermission(handlerMethod, currentUser);
        
        log.debug("用户认证成功, userId: {}, role: {}, URI: {}", 
                  currentUser.getId(), currentUser.getUserRole(), request.getRequestURI());
        return true;
    }

    /**
     * 检查是否需要登录
     */
    private boolean checkNeedLogin(HandlerMethod handlerMethod) {
        // 先检查方法级别的@RequireLogin注解
        RequireLogin methodLoginAnnotation = handlerMethod.getMethodAnnotation(RequireLogin.class);
        if (methodLoginAnnotation != null) {
            return methodLoginAnnotation.required();
        }

        // 再检查类级别的@RequireLogin注解
        RequireLogin classLoginAnnotation = handlerMethod.getBeanType().getAnnotation(RequireLogin.class);
        if (classLoginAnnotation != null) {
            return classLoginAnnotation.required();
        }

        // 如果有@RequireRole注解，自动需要登录
        RequireRole methodRoleAnnotation = handlerMethod.getMethodAnnotation(RequireRole.class);
        RequireRole classRoleAnnotation = handlerMethod.getBeanType().getAnnotation(RequireRole.class);
        
        return methodRoleAnnotation != null || classRoleAnnotation != null;
    }

    /**
     * 验证登录并获取用户信息
     */
    private SysUser validateLoginAndGetUser(HttpServletRequest request) {
        // 1. 从Header获取Token
        String token = request.getHeader(TOKEN_HEADER);
        if (token == null || !token.startsWith(TOKEN_PREFIX)) {
            log.warn("未提供有效Token, URI: {}", request.getRequestURI());
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        
        token = token.substring(TOKEN_PREFIX.length());

        // 2. 验证Token是否有效
        String redisKey = REDIS_LOGIN_KEY + token;
        String userIdStr = stringRedisTemplate.opsForValue().get(redisKey);
        
        if (userIdStr == null) {
            log.warn("Token已过期或无效, token: {}", token);
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }

        // 3. 刷新Token过期时间（30天）
        stringRedisTemplate.expire(redisKey, 30, TimeUnit.DAYS);

        // 4. 查询用户信息
        Long userId = Long.parseLong(userIdStr);
        SysUser user = sysUserService.getById(userId);
        
        if (user == null) {
            log.error("用户不存在, userId: {}", userId);
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "用户不存在");
        }

        return user;
    }

    /**
     * 检查角色权限
     */
    private void checkRolePermission(HandlerMethod handlerMethod, SysUser currentUser) {
        // 先检查方法级别的@RequireRole注解
        RequireRole methodRoleAnnotation = handlerMethod.getMethodAnnotation(RequireRole.class);
        RequireRole targetAnnotation = methodRoleAnnotation;

        // 如果方法上没有，检查类级别
        if (targetAnnotation == null) {
            targetAnnotation = handlerMethod.getBeanType().getAnnotation(RequireRole.class);
        }

        // 没有角色要求，直接通过
        if (targetAnnotation == null) {
            return;
        }

        // 获取当前用户角色
        UserRole currentRole = UserRole.fromValue(currentUser.getUserRole());
        UserRole[] requiredRoles = targetAnnotation.value();

        // 检查用户角色是否在允许的角色列表中
        boolean hasPermission = Arrays.asList(requiredRoles).contains(currentRole);

        if (!hasPermission) {
            log.warn("权限不足, userId: {}, currentRole: {}, requiredRoles: {}", 
                     currentUser.getId(), currentRole, Arrays.toString(requiredRoles));
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "权限不足");
        }
    }
}
