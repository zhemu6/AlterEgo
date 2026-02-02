package org.zhemu.alterego.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.redis.connection.ReturnType;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.zhemu.alterego.annotation.RateLimit;
import org.zhemu.alterego.constant.RedisConstants;
import org.zhemu.alterego.exception.BusinessException;
import org.zhemu.alterego.exception.ErrorCode;
import org.zhemu.alterego.util.IpUtils;

import java.nio.charset.StandardCharsets;

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

    // Lua 脚本：原子性地检查和增加计数
    // KEYS[1]: Redis key
    // ARGV[1]: 过期时间（秒）
    // ARGV[2]: 最大请求次数
    // 返回值：当前计数（-1表示超限）
    private static final String RATE_LIMIT_LUA_SCRIPT =
            "local current = redis.call('GET', KEYS[1]) " +
                    "if current == false then " +
                    "  redis.call('SET', KEYS[1], 1, 'EX', ARGV[1]) " +
                    "  return 1 " +
                    "else " +
                    "  local count = tonumber(current) " +
                    "  if count >= tonumber(ARGV[2]) then " +
                    "    return -1 " +
                    "  else " +
                    "    redis.call('INCR', KEYS[1]) " +
                    "    return count + 1 " +
                    "  end " +
                    "end";

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
        boolean limitByIp = rateLimit.limitByIp();
        boolean limitByEmail = rateLimit.limitByEmail();

        String clientIp = null;
        if (limitByIp) {
            clientIp = IpUtils.getClientIp(request);
        }

        String email = null;
        if (limitByEmail) {
            email = extractEmail(request);
            if (email != null && email.isEmpty()) {
                email = null;
            }
        }

        // 同时开启 IP + Email 时：以 (ip, email) 组合作为维度计数，避免不同邮箱互相影响
        if (limitByIp && limitByEmail && email != null) {
            String key = RedisConstants.RATE_LIMIT_PREFIX + rateLimit.key()
                    + ":ip:" + clientIp
                    + ":email:" + email;
            checkLimit(key, rateLimit, "IP: " + clientIp + ", Email: " + email);
            return;
        }

        // 仅 IP，或 Email 缺失时回退到 IP
        if (limitByIp) {
            String key = RedisConstants.RATE_LIMIT_PREFIX + rateLimit.key() + ":ip:" + clientIp;
            checkLimit(key, rateLimit, "IP: " + clientIp);
        }

        // 仅 Email（且能取到 email）
        if (!limitByIp && limitByEmail && email != null) {
            String key = RedisConstants.RATE_LIMIT_PREFIX + rateLimit.key() + ":email:" + email;
            checkLimit(key, rateLimit, "Email: " + email);
        }
    }

    /**
     * 从请求中提取邮箱地址
     * 支持从查询参数和请求体中提取
     */
    private String extractEmail(HttpServletRequest request) {
        // 先尝试从查询参数获取
        String email = request.getParameter("email");
        if (email != null && !email.isEmpty()) {
            return email;
        }

        // TODO: 如果需要支持从JSON请求体中提取email，可以在这里实现
        // 注意：从请求体读取需要缓存，因为请求体流只能读取一次
        // 建议在需要时使用 ContentCachingRequestWrapper

        return null;
    }

    /**
     * 检查指定key的访问频率是否超限
     * 使用 Lua 脚本保证原子性，避免并发问题
     */
    private void checkLimit(String key, RateLimit rateLimit, String identifier) {
        // 执行 Lua 脚本
        Long count = stringRedisTemplate.execute(
                (RedisCallback<Long>) connection -> {
                    return connection.scriptingCommands().eval(
                            RATE_LIMIT_LUA_SCRIPT.getBytes(StandardCharsets.UTF_8),
                            ReturnType.INTEGER,
                            1,
                            key.getBytes(StandardCharsets.UTF_8),
                            String.valueOf(rateLimit.timeWindow()).getBytes(StandardCharsets.UTF_8),
                            String.valueOf(rateLimit.maxCount()).getBytes(StandardCharsets.UTF_8)
                    );
                }
        );

        if (count == null) {
            // Redis 执行失败，记录错误但不阻塞请求（fail-open 策略）
            log.error("Redis执行失败，限流检查跳过: key={}, identifier={}", key, identifier);
            return;
        }

        if (count == -1) {
            // 达到限流阈值
            log.warn("请求频率超限: key={}, identifier={}, maxCount={}",
                    key, identifier, rateLimit.maxCount());
            throw new BusinessException(ErrorCode.TOO_MANY_REQUEST,
                    "请求过于频繁，请稍后再试");
        }

        log.debug("限流记录更新: key={}, identifier={}, count={}", key, identifier, count);
    }
}
