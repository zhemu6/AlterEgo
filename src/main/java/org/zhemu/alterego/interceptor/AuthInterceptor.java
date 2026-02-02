package org.zhemu.alterego.interceptor;

import cn.hutool.json.JSONUtil;
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
import org.zhemu.alterego.constant.RedisConstants;
import org.zhemu.alterego.exception.BusinessException;
import org.zhemu.alterego.exception.ErrorCode;
import org.zhemu.alterego.model.entity.SysUser;
import org.zhemu.alterego.model.enums.UserRoleEnum;
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
     * 验证登录并获取用户信息（优化：优先从缓存读取，避免查库）
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
        String redisKey = RedisConstants.USER_LOGIN_TOKEN + token;
        String userIdStr = stringRedisTemplate.opsForValue().get(redisKey);
        
        if (userIdStr == null) {
            log.warn("Token已过期或无效, token: {}", token);
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }

        // 3. 刷新Token过期时间（30天）
        stringRedisTemplate.expire(redisKey, RedisConstants.USER_LOGIN_TOKEN_TTL, TimeUnit.DAYS);

        // 4. 优先从缓存获取用户信息（性能优化：避免每次请求查库）
        Long userId = Long.parseLong(userIdStr);
        String userCacheKey = RedisConstants.USER_INFO_CACHE + userId;
        String userJson = stringRedisTemplate.opsForValue().get(userCacheKey);
        
        SysUser user;
        if (userJson != null) {
            // 缓存命中，直接反序列化
            user = JSONUtil.toBean(userJson, SysUser.class);
            // 刷新用户缓存过期时间
            stringRedisTemplate.expire(userCacheKey, RedisConstants.USER_INFO_CACHE_TTL, TimeUnit.DAYS);
            log.debug("从缓存获取用户信息, userId: {}", userId);
        } else {
            // 缓存未命中，查询数据库并回写缓存
            user = sysUserService.getById(userId);
            if (user == null) {
                log.error("用户不存在, userId: {}", userId);
                throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "用户不存在");
            }
            
            // 回写缓存（密码字段置空）
            user.setUserPassword(null);
            String userJsonForCache = JSONUtil.toJsonStr(user);
            stringRedisTemplate.opsForValue().set(
                    userCacheKey,
                    userJsonForCache,
                    RedisConstants.USER_INFO_CACHE_TTL,
                    TimeUnit.DAYS
            );
            log.debug("用户缓存未命中，已回写缓存, userId: {}", userId);
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

        // 获取当前用户角色（安全性：fromValue可能返回null）
        UserRoleEnum currentRole = UserRoleEnum.fromValue(currentUser.getUserRole());
        
        // 安全检查：如果角色无效，拒绝访问
        if (currentRole == null) {
            log.warn("无效的用户角色, userId: {}, role: {}", 
                     currentUser.getId(), currentUser.getUserRole());
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "用户角色无效");
        }
        
        UserRoleEnum[] requiredRoles = targetAnnotation.value();

        // 检查用户角色是否在允许的角色列表中
        boolean hasPermission = Arrays.asList(requiredRoles).contains(currentRole);

        if (!hasPermission) {
            log.warn("权限不足, userId: {}, currentRole: {}, requiredRoles: {}", 
                     currentUser.getId(), currentRole, Arrays.toString(requiredRoles));
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "权限不足");
        }
    }
}
