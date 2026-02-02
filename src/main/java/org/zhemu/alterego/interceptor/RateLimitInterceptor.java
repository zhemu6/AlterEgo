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
import org.zhemu.alterego.annotation.RateLimit;
import org.zhemu.alterego.constant.RedisConstants;
import org.zhemu.alterego.exception.BusinessException;
import org.zhemu.alterego.exception.ErrorCode;
import org.zhemu.alterego.util.IpUtils;

import java.util.concurrent.TimeUnit;

/**
 * 接口限流拦截器
 * 基于Redis实现分布式限流
 * 
 * @author lushihao
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RateLimitInterceptor implements HandlerInterceptor {

    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public boolean preHandle(@NotNull HttpServletRequest request,
                             @NotNull HttpServletResponse response,
                             Object handler) throws Exception {
        
        // 1. 不是Controller方法直接放行
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }

        // 2. 检查是否有@RateLimit注解
        RateLimit rateLimit = handlerMethod.getMethodAnnotation(RateLimit.class);
        if (rateLimit == null) {
            // 没有限流注解，直接放行
            return true;
        }

        // 3. 执行限流检查
        checkRateLimit(request, rateLimit);
        
        return true;
    }

    /**
     * 执行限流检查
     */
    private void checkRateLimit(HttpServletRequest request, RateLimit rateLimit) {
        // 基于IP限流
        if (rateLimit.limitByIp()) {
            String clientIp = IpUtils.getClientIp(request);
            String ipKey = RedisConstants.RATE_LIMIT_PREFIX + rateLimit.key() + ":ip:" + clientIp;
            checkLimit(ipKey, rateLimit, "IP: " + clientIp);
        }

        // 基于邮箱限流
        if (rateLimit.limitByEmail()) {
            String email = request.getParameter("email");
            if (email != null && !email.isEmpty()) {
                String emailKey = RedisConstants.RATE_LIMIT_PREFIX + rateLimit.key() + ":email:" + email;
                checkLimit(emailKey, rateLimit, "Email: " + email);
            }
        }
    }

    /**
     * 检查指定key的访问频率是否超限
     */
    private void checkLimit(String key, RateLimit rateLimit, String identifier) {
        // 获取当前访问次数
        String countStr = stringRedisTemplate.opsForValue().get(key);
        
        if (countStr == null) {
            // 第一次访问，设置计数为1，并设置过期时间
            stringRedisTemplate.opsForValue().set(key, "1", rateLimit.timeWindow(), TimeUnit.SECONDS);
            log.debug("限流记录创建: key={}, identifier={}", key, identifier);
            return;
        }

        // 检查是否超过限制
        int count = Integer.parseInt(countStr);
        if (count >= rateLimit.maxCount()) {
            log.warn("请求频率超限: key={}, identifier={}, count={}, maxCount={}", 
                     key, identifier, count, rateLimit.maxCount());
            throw new BusinessException(ErrorCode.TOO_MANY_REQUEST, 
                                      "请求过于频繁，请稍后再试");
        }

        // 增加计数
        stringRedisTemplate.opsForValue().increment(key);
        log.debug("限流记录更新: key={}, identifier={}, count={}", key, identifier, count + 1);
    }
}
