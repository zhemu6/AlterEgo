package org.zhemu.alterego.service.impl;

import cn.hutool.core.util.RandomUtil;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.zhemu.alterego.constant.RedisConstants;
import org.zhemu.alterego.exception.BusinessException;
import org.zhemu.alterego.model.dto.user.SysUserLoginRequest;
import org.zhemu.alterego.model.dto.user.SysUserPasswordResetRequest;
import org.zhemu.alterego.model.dto.user.SysUserRegisterRequest;
import org.zhemu.alterego.model.dto.user.SysUserUpdatePasswordRequest;
import org.zhemu.alterego.model.entity.SysUser;
import org.zhemu.alterego.model.vo.SysUserVO;
import org.zhemu.alterego.service.SysUserService;

import jakarta.servlet.http.HttpServletRequest;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 用户服务单元测试
 * 
 * @author lushihao
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class SysUserServiceImplTest {

    @Autowired
    private SysUserService sysUserService;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    // 测试数据
    private static final String TEST_ACCOUNT = "testuser_" + System.currentTimeMillis();
    private static final String TEST_EMAIL = "test_" + System.currentTimeMillis() + "@example.com";
    private static final String TEST_PASSWORD = "password123";
    private static String TEST_CODE = "123456";
    private static String TEST_TOKEN = "";
    private static Long TEST_USER_ID = null;

    @BeforeEach
    void setUp() {
        // 每次测试前生成新的验证码
        TEST_CODE = RandomUtil.randomNumbers(6);
    }

    @Test
    @Order(1)
    @DisplayName("测试1：用户注册 - 成功")
    void testRegisterSuccess() {
        // 1. 准备验证码（模拟邮件发送）
        String codeKey = RedisConstants.LOGIN_EMAIL_CODE + TEST_EMAIL;
        stringRedisTemplate.opsForValue().set(codeKey, TEST_CODE, 5, TimeUnit.MINUTES);

        // 2. 准备注册请求
        SysUserRegisterRequest request = SysUserRegisterRequest.builder()
                .userAccount(TEST_ACCOUNT)
                .userPassword(TEST_PASSWORD)
                .checkPassword(TEST_PASSWORD)
                .email(TEST_EMAIL)
                .code(TEST_CODE)
                .build();

        // 3. 执行注册
        Long userId = sysUserService.register(request);

        // 4. 验证结果
        assertNotNull(userId, "注册应该返回用户ID");
        assertTrue(userId > 0, "用户ID应该大于0");
        
        // 5. 验证验证码已删除
        String cachedCode = stringRedisTemplate.opsForValue().get(codeKey);
        assertNull(cachedCode, "注册成功后验证码应该被删除");

        // 保存用户ID供后续测试使用
        TEST_USER_ID = userId;
        
        System.out.println("✓ 注册成功，用户ID: " + userId);
    }

    @Test
    @Order(2)
    @DisplayName("测试2：用户注册 - 验证码错误")
    void testRegisterWithWrongCode() {
        // 1. 准备验证码
        String codeKey = RedisConstants.LOGIN_EMAIL_CODE + "wrong@example.com";
        stringRedisTemplate.opsForValue().set(codeKey, "111111", 5, TimeUnit.MINUTES);

        // 2. 准备注册请求（使用错误的验证码）
        SysUserRegisterRequest request = SysUserRegisterRequest.builder()
                .userAccount("wronguser")
                .userPassword(TEST_PASSWORD)
                .checkPassword(TEST_PASSWORD)
                .email("wrong@example.com")
                .code("999999") // 错误的验证码
                .build();

        // 3. 验证抛出异常
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            sysUserService.register(request);
        });

        assertTrue(exception.getMessage().contains("验证码错误"), "应该提示验证码错误");
        
        System.out.println("✓ 验证码错误时正确抛出异常");
    }

    @Test
    @Order(3)
    @DisplayName("测试3：账号密码登录 - 成功")
    void testLoginByAccount() {
        // 1. 准备登录请求
        SysUserLoginRequest request = SysUserLoginRequest.builder()
                .type("account")
                .userAccount(TEST_ACCOUNT)
                .userPassword(TEST_PASSWORD)
                .build();

        // 2. Mock HttpServletRequest
        HttpServletRequest httpRequest = mock(HttpServletRequest.class);

        // 3. 执行登录
        SysUserVO userVO = sysUserService.login(request, httpRequest);

        // 4. 验证结果
        assertNotNull(userVO, "登录应该返回用户信息");
        assertEquals(TEST_ACCOUNT, userVO.getUserAccount(), "返回的账号应该匹配");
        assertEquals(TEST_EMAIL, userVO.getEmail(), "返回的邮箱应该匹配");

        // 5. 获取 token
        when(httpRequest.getAttribute("token")).thenReturn("mock-token");
        String token = (String) httpRequest.getAttribute("token");
        if (token != null) {
            TEST_TOKEN = token;
        }

        System.out.println("✓ 账号密码登录成功，用户: " + userVO.getUserAccount());
    }

    @Test
    @Order(4)
    @DisplayName("测试4：账号密码登录 - 密码错误")
    void testLoginWithWrongPassword() {
        // 1. 准备登录请求（错误密码）
        SysUserLoginRequest request = SysUserLoginRequest.builder()
                .type("account")
                .userAccount(TEST_ACCOUNT)
                .userPassword("wrongpassword")
                .build();

        HttpServletRequest httpRequest = mock(HttpServletRequest.class);

        // 2. 验证抛出异常
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            sysUserService.login(request, httpRequest);
        });

        assertTrue(exception.getMessage().contains("账号或密码错误"), "应该提示账号或密码错误");
        
        System.out.println("✓ 密码错误时正确抛出异常");
    }

    @Test
    @Order(5)
    @DisplayName("测试5：邮箱验证码登录 - 成功")
    void testLoginByEmail() {
        // 1. 准备验证码
        String codeKey = RedisConstants.LOGIN_EMAIL_CODE + TEST_EMAIL;
        stringRedisTemplate.opsForValue().set(codeKey, TEST_CODE, 5, TimeUnit.MINUTES);

        // 2. 准备登录请求
        SysUserLoginRequest request = SysUserLoginRequest.builder()
                .type("email")
                .email(TEST_EMAIL)
                .code(TEST_CODE)
                .build();

        HttpServletRequest httpRequest = mock(HttpServletRequest.class);

        // 3. 执行登录
        SysUserVO userVO = sysUserService.login(request, httpRequest);

        // 4. 验证结果
        assertNotNull(userVO, "登录应该返回用户信息");
        assertEquals(TEST_EMAIL, userVO.getEmail(), "返回的邮箱应该匹配");

        // 5. 验证验证码已删除
        String cachedCode = stringRedisTemplate.opsForValue().get(codeKey);
        assertNull(cachedCode, "登录成功后验证码应该被删除");

        System.out.println("✓ 邮箱验证码登录成功");
    }

    @Test
    @Order(6)
    @DisplayName("测试6：修改密码 - 成功")
    void testUpdatePassword() {
        // 1. 模拟用户已登录（设置 RequestContext）
        MockHttpServletRequest httpRequest = new MockHttpServletRequest();
        httpRequest.setAttribute("userId", TEST_USER_ID);
        httpRequest.setAttribute("userRole", "user");
        
        // 查询用户信息并设置到 request
        SysUser currentUser = sysUserService.getById(TEST_USER_ID);
        httpRequest.setAttribute("currentUser", currentUser);
        
        // 设置到 Spring 的 RequestContextHolder
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(httpRequest));

        // 2. 准备修改密码请求
        String newPassword = "newpassword456";
        SysUserUpdatePasswordRequest request = new SysUserUpdatePasswordRequest();
        request.setOldPassword(TEST_PASSWORD);
        request.setNewPassword(newPassword);
        request.setCheckPassword(newPassword);

        // 3. 执行修改密码（不再需要传 httpRequest）
        boolean result = sysUserService.updatePassword(request);

        // 4. 验证结果
        assertTrue(result, "修改密码应该成功");

        // 5. 验证新密码可以登录
        SysUserLoginRequest loginRequest = SysUserLoginRequest.builder()
                .type("account")
                .userAccount(TEST_ACCOUNT)
                .userPassword(newPassword)
                .build();

        SysUserVO userVO = sysUserService.login(loginRequest, mock(HttpServletRequest.class));
        assertNotNull(userVO, "使用新密码应该可以登录");
        
        // 清理 RequestContext
        RequestContextHolder.resetRequestAttributes();

        System.out.println("✓ 修改密码成功，新密码可以登录");
    }

    @Test
    @Order(7)
    @DisplayName("测试7：重置密码 - 成功")
    void testResetPassword() {
        // 1. 准备验证码
        String codeKey = RedisConstants.LOGIN_EMAIL_CODE + TEST_EMAIL;
        stringRedisTemplate.opsForValue().set(codeKey, TEST_CODE, 5, TimeUnit.MINUTES);

        // 2. 准备重置密码请求
        String resetPassword = "resetpassword789";
        SysUserPasswordResetRequest request = new SysUserPasswordResetRequest();
        request.setEmail(TEST_EMAIL);
        request.setCode(TEST_CODE);
        request.setNewPassword(resetPassword);
        request.setCheckPassword(resetPassword);

        // 3. 执行重置密码
        boolean result = sysUserService.resetPassword(request);

        // 4. 验证结果
        assertTrue(result, "重置密码应该成功");

        // 5. 验证新密码可以登录
        SysUserLoginRequest loginRequest = SysUserLoginRequest.builder()
                .type("account")
                .userAccount(TEST_ACCOUNT)
                .userPassword(resetPassword)
                .build();

        SysUserVO userVO = sysUserService.login(loginRequest, mock(HttpServletRequest.class));
        assertNotNull(userVO, "使用重置后的密码应该可以登录");

        System.out.println("✓ 重置密码成功，新密码可以登录");
    }

    @Test
    @Order(8)
    @DisplayName("测试8：获取当前用户")
    void testGetCurrentUser() {
        // 1. 模拟用户已登录（设置 RequestContext）
        MockHttpServletRequest httpRequest = new MockHttpServletRequest();
        httpRequest.setAttribute("userId", TEST_USER_ID);
        httpRequest.setAttribute("userRole", "user");
        
        // 查询用户信息并设置到 request
        SysUser currentUser = sysUserService.getById(TEST_USER_ID);
        httpRequest.setAttribute("currentUser", currentUser);
        
        // 设置到 Spring 的 RequestContextHolder
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(httpRequest));

        // 2. 执行获取当前用户（不再需要传 httpRequest）
        SysUserVO userVO = sysUserService.getCurrentUser();

        // 3. 验证结果
        assertNotNull(userVO, "应该返回用户信息");
        assertEquals(TEST_ACCOUNT, userVO.getUserAccount(), "账号应该匹配");
        assertEquals(TEST_EMAIL, userVO.getEmail(), "邮箱应该匹配");
        
        // 清理 RequestContext
        RequestContextHolder.resetRequestAttributes();

        System.out.println("✓ 获取当前用户成功: " + userVO.getUserAccount());
    }

    @AfterAll
    static void tearDown(@Autowired SysUserService sysUserService) {
        // 清理测试数据（可选）
        if (TEST_USER_ID != null) {
            sysUserService.removeById(TEST_USER_ID);
            System.out.println("\n✓ 测试数据已清理，删除用户ID: " + TEST_USER_ID);
        }
    }
}
