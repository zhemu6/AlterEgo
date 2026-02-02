package org.zhemu.alterego.interceptor;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.method.HandlerMethod;
import org.zhemu.alterego.annotation.RateLimit;
import org.zhemu.alterego.constant.RedisConstants;
import org.zhemu.alterego.exception.BusinessException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 限流拦截器单元测试
 *
 * @author lushihao
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class RateLimitInterceptorTest {

    @Autowired
    private RateLimitInterceptor rateLimitInterceptor;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();

        // 清理测试用的Redis key
        // 使用 SCAN 代替 KEYS，避免在生产环境中阻塞 Redis
        String pattern = RedisConstants.RATE_LIMIT_PREFIX + "test_*";
        stringRedisTemplate.execute((org.springframework.data.redis.core.RedisCallback<Void>) connection -> {
            var cursor = connection.scan(
                    org.springframework.data.redis.core.ScanOptions.scanOptions()
                            .match(pattern)
                            .count(100)
                            .build()
            );
            while (cursor.hasNext()) {
                connection.del(cursor.next());
            }
            return null;
        });
    }

    @Test
    void testPreHandle_noRateLimitAnnotation_shouldAllow() throws Exception {
        // 没有@RateLimit注解的handler应该直接放行
        Object handler = new Object();

        boolean result = rateLimitInterceptor.preHandle(request, response, handler);

        assertTrue(result, "Should allow request without @RateLimit annotation");
    }

    @Test
    void testPreHandle_withRateLimitByIp_shouldAllow() throws Exception {
        // 创建带@RateLimit注解的mock handler
        HandlerMethod handler = createMockHandler();
        request.setRemoteAddr("192.168.1.1");

        // 第一次请求应该成功
        boolean result = rateLimitInterceptor.preHandle(request, response, handler);

        assertTrue(result, "First request should be allowed");
    }

    @Test
    void testPreHandle_withRateLimitByIp_shouldBlock() throws Exception {
        // 创建限制为2次/60秒的handler
        HandlerMethod handler = createMockHandler();
        request.setRemoteAddr("192.168.1.2");

        // 前两次应该成功
        assertTrue(rateLimitInterceptor.preHandle(request, response, handler));
        assertTrue(rateLimitInterceptor.preHandle(request, response, handler));

        // 第三次应该被拦截
        assertThrows(BusinessException.class, () -> {
            rateLimitInterceptor.preHandle(request, response, handler);
        }, "Third request should be blocked by rate limit");
    }

    @Test
    void testPreHandle_withRateLimitByEmail_shouldAllow() throws Exception {
        // 创建基于邮箱限流的handler
        HandlerMethod handler = createMockHandler();
        request.setParameter("email", "test@example.com");
        request.setRemoteAddr("192.168.1.3");

        // 第一次请求应该成功
        boolean result = rateLimitInterceptor.preHandle(request, response, handler);

        assertTrue(result, "First request should be allowed");
    }

    @Test
    void testPreHandle_withRateLimitByEmail_shouldBlock() throws Exception {
        // 创建限制为2次/60秒的handler
        HandlerMethod handler = createMockHandler();
        request.setParameter("email", "test2@example.com");
        request.setRemoteAddr("192.168.1.4");

        // 前两次应该成功
        assertTrue(rateLimitInterceptor.preHandle(request, response, handler));
        assertTrue(rateLimitInterceptor.preHandle(request, response, handler));

        // 第三次应该被拦截
        assertThrows(BusinessException.class, () -> {
            rateLimitInterceptor.preHandle(request, response, handler);
        }, "Third request should be blocked by rate limit");
    }

    @Test
    void testPreHandle_bothIpAndEmail_shouldBlockOnEither() throws Exception {
        // 创建同时基于IP和邮箱限流的handler，限制为2次
        HandlerMethod handler = createMockHandler();
        request.setParameter("email", "test3@example.com");
        request.setRemoteAddr("192.168.1.5");

        // 前两次应该成功
        assertTrue(rateLimitInterceptor.preHandle(request, response, handler));
        assertTrue(rateLimitInterceptor.preHandle(request, response, handler));

        // 第三次应该被IP限流拦截
        assertThrows(BusinessException.class, () -> {
            rateLimitInterceptor.preHandle(request, response, handler);
        }, "Should be blocked by IP rate limit");
    }

    @Test
    void testPreHandle_differentIps_shouldAllowSeparately() throws Exception {
        // 不同IP应该分别计数
        HandlerMethod handler = createMockHandler();

        // IP1 发送2次请求
        request.setRemoteAddr("192.168.1.10");
        assertTrue(rateLimitInterceptor.preHandle(request, response, handler));
        assertTrue(rateLimitInterceptor.preHandle(request, response, handler));

        // IP2 应该还可以发送请求
        request.setRemoteAddr("192.168.1.11");
        assertTrue(rateLimitInterceptor.preHandle(request, response, handler));
    }

    @Test
    void testPreHandle_differentEmails_shouldAllowSeparately() throws Exception {
        // 不同邮箱应该分别计数
        HandlerMethod handler = createMockHandler();

        // Email1 发送2次请求
        MockHttpServletRequest request1 = new MockHttpServletRequest();
        request1.setRemoteAddr("192.168.1.12");
        request1.setParameter("email", "user1@example.com");
        assertTrue(rateLimitInterceptor.preHandle(request1, response, handler));
        assertTrue(rateLimitInterceptor.preHandle(request1, response, handler));

        // Email2 应该还可以发送请求（使用新的 request 对象确保隔离）
        MockHttpServletRequest request2 = new MockHttpServletRequest();
        request2.setRemoteAddr("192.168.1.12");
        request2.setParameter("email", "user2@example.com");
        assertTrue(rateLimitInterceptor.preHandle(request2, response, handler));
    }

    /**
     * 创建带@RateLimit注解的mock HandlerMethod
     */
    private HandlerMethod createMockHandler() {
        try {
            // 创建一个测试用的controller方法
            var method = TestController.class.getMethod("testMethod");
            var controller = new TestController();
            return new HandlerMethod(controller, method);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 测试用的Controller类
     */
    public static class TestController {
        @RateLimit(key = "test_endpoint", maxCount = 2, timeWindow = 60,
                limitByIp = true, limitByEmail = true)
        public void testMethod() {
            // 测试方法
        }
    }
}