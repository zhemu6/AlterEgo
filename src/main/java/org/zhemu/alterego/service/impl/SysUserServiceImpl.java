package org.zhemu.alterego.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.zhemu.alterego.constant.RedisConstants;
import org.zhemu.alterego.exception.BusinessException;
import org.zhemu.alterego.exception.ErrorCode;
import org.zhemu.alterego.exception.ThrowUtils;
import org.zhemu.alterego.mapper.SysUserMapper;
import org.zhemu.alterego.model.dto.user.SysUserLoginRequest;
import org.zhemu.alterego.model.dto.user.SysUserPasswordResetRequest;
import org.zhemu.alterego.model.dto.user.SysUserRegisterRequest;
import org.zhemu.alterego.model.dto.user.SysUserUpdatePasswordRequest;
import org.zhemu.alterego.model.entity.SysUser;
import org.zhemu.alterego.model.enums.LoginTypeEnum;
import org.zhemu.alterego.model.enums.UserRoleEnum;
import org.zhemu.alterego.model.enums.UserStatusEnum;
import org.zhemu.alterego.model.vo.SysUserVO;
import org.zhemu.alterego.service.SysUserService;
import org.zhemu.alterego.util.UserContext;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * @author lushihao
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SysUserServiceImpl extends ServiceImpl<SysUserMapper, SysUser>
        implements SysUserService {

    private final StringRedisTemplate stringRedisTemplate;
    private final PasswordEncoder passwordEncoder;

    @Override
    public Long register(SysUserRegisterRequest request) {
        // 1. 参数校验
        String userAccount = request.getUserAccount();
        String userPassword = request.getUserPassword();
        String checkPassword = request.getCheckPassword();
        String email = request.getEmail();
        String code = request.getCode();

        ThrowUtils.throwIf(StrUtil.isBlank(userAccount), ErrorCode.PARAMS_ERROR, "账号不能为空");
        ThrowUtils.throwIf(StrUtil.isBlank(userPassword), ErrorCode.PARAMS_ERROR, "密码不能为空");
        ThrowUtils.throwIf(StrUtil.isBlank(email), ErrorCode.PARAMS_ERROR, "邮箱不能为空");
        ThrowUtils.throwIf(StrUtil.isBlank(code), ErrorCode.PARAMS_ERROR, "验证码不能为空");

        // 2. 校验两次密码是否一致
        ThrowUtils.throwIf(!userPassword.equals(checkPassword), ErrorCode.PARAMS_ERROR, "两次密码不一致");

        // 3. 校验验证码
        String codeKey = RedisConstants.LOGIN_EMAIL_CODE + email;
        String cachedCode = stringRedisTemplate.opsForValue().get(codeKey);
        ThrowUtils.throwIf(StrUtil.isBlank(cachedCode), ErrorCode.PARAMS_ERROR, "验证码已过期");
        ThrowUtils.throwIf(!code.equals(cachedCode), ErrorCode.PARAMS_ERROR, "验证码错误");

        // 4. 校验账号是否已存在
        LambdaQueryWrapper<SysUser> accountQuery = new LambdaQueryWrapper<>();
        accountQuery.eq(SysUser::getUserAccount, userAccount);
        long accountCount = this.count(accountQuery);
        ThrowUtils.throwIf(accountCount > 0, ErrorCode.PARAMS_ERROR, "账号已存在");

        // 5. 校验邮箱是否已存在
        LambdaQueryWrapper<SysUser> emailQuery = new LambdaQueryWrapper<>();
        emailQuery.eq(SysUser::getEmail, email);
        long emailCount = this.count(emailQuery);
        ThrowUtils.throwIf(emailCount > 0, ErrorCode.PARAMS_ERROR, "邮箱已被注册");

        // 6. 加密密码
        String encryptedPassword = passwordEncoder.encode(userPassword);

        // 7. 创建用户
        SysUser newUser = SysUser.builder()
                .userAccount(userAccount)
                .userPassword(encryptedPassword)
                .email(email)
                .userRole(UserRoleEnum.USER.getValue())
                .userStatus(UserStatusEnum.Normal.getValue())
                .build();

        boolean saved = this.save(newUser);
        ThrowUtils.throwIf(!saved, ErrorCode.SYSTEM_ERROR, "注册失败");

        // 8. 删除验证码
        stringRedisTemplate.delete(codeKey);

        log.info("用户注册成功，账号：{}, ID：{}", userAccount, newUser.getId());
        return newUser.getId();
    }

    @Override
    public SysUserVO login(SysUserLoginRequest request, HttpServletRequest httpRequest) {
        String type = request.getType();
        LoginTypeEnum loginType = LoginTypeEnum.getEnumByValue(type);
        ThrowUtils.throwIf(loginType == null, ErrorCode.PARAMS_ERROR, "登录方式错误");

        SysUser user;
        switch (loginType) {
            case Account:
                user = loginByAccount(request);
                break;
            case Email:
                user = loginByEmail(request);
                break;
            default:
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "不支持的登录方式");
        }

        // 生成 token 并存入 Redis
        String token = UUID.randomUUID().toString();
        String tokenKey = RedisConstants.USER_LOGIN_TOKEN + token;
        
        // 存储 token -> userId 映射（保持兼容性）
        stringRedisTemplate.opsForValue().set(
                tokenKey,
                String.valueOf(user.getId()),
                RedisConstants.USER_LOGIN_TOKEN_TTL,
                TimeUnit.DAYS
        );
        
        // 缓存完整用户对象，避免每次请求查库（性能优化）
        String userCacheKey = RedisConstants.USER_INFO_CACHE + user.getId();
        // 将密码字段置空，避免缓存敏感信息
        user.setUserPassword(null);
        String userJson = JSONUtil.toJsonStr(user);
        stringRedisTemplate.opsForValue().set(
                userCacheKey,
                userJson,
                RedisConstants.USER_INFO_CACHE_TTL,
                TimeUnit.DAYS
        );

        // 返回用户信息和 token
        SysUserVO userVO = SysUserVO.objToVo(user);
        userVO.setToken(token); // 将token放入VO中返回
        // 在响应头中设置 token (Controller 层处理，这里只返回 VO)
        httpRequest.setAttribute("token", token);

        log.info("用户登录成功，账号：{}, ID：{}, token：{}", user.getUserAccount(), user.getId(), token);
        return userVO;
    }

    /**
     * 账号密码登录
     */
    private SysUser loginByAccount(SysUserLoginRequest request) {
        String userAccount = request.getUserAccount();
        String userPassword = request.getUserPassword();

        ThrowUtils.throwIf(StrUtil.isBlank(userAccount), ErrorCode.PARAMS_ERROR, "账号不能为空");
        ThrowUtils.throwIf(StrUtil.isBlank(userPassword), ErrorCode.PARAMS_ERROR, "密码不能为空");

        // 查询用户
        LambdaQueryWrapper<SysUser> query = new LambdaQueryWrapper<>();
        query.eq(SysUser::getUserAccount, userAccount);
        SysUser user = this.getOne(query);
        ThrowUtils.throwIf(user == null, ErrorCode.PARAMS_ERROR, "账号或密码错误");

        // 校验密码
        boolean matches = passwordEncoder.matches(userPassword, user.getUserPassword());
        ThrowUtils.throwIf(!matches, ErrorCode.PARAMS_ERROR, "账号或密码错误");

        // 校验用户状态（使用 Objects.equals 避免装箱问题和NPE）
        ThrowUtils.throwIf(java.util.Objects.equals(user.getUserStatus(), UserStatusEnum.Baned.getValue()),
                ErrorCode.FORBIDDEN_ERROR, "账号已被禁用");

        return user;
    }

    /**
     * 邮箱验证码登录
     */
    private SysUser loginByEmail(SysUserLoginRequest request) {
        String email = request.getEmail();
        String code = request.getCode();

        ThrowUtils.throwIf(StrUtil.isBlank(email), ErrorCode.PARAMS_ERROR, "邮箱不能为空");
        ThrowUtils.throwIf(StrUtil.isBlank(code), ErrorCode.PARAMS_ERROR, "验证码不能为空");

        // 校验验证码
        String codeKey = RedisConstants.LOGIN_EMAIL_CODE + email;
        String cachedCode = stringRedisTemplate.opsForValue().get(codeKey);
        ThrowUtils.throwIf(StrUtil.isBlank(cachedCode), ErrorCode.PARAMS_ERROR, "验证码已过期");
        ThrowUtils.throwIf(!code.equals(cachedCode), ErrorCode.PARAMS_ERROR, "验证码错误");

        // 查询用户
        LambdaQueryWrapper<SysUser> query = new LambdaQueryWrapper<>();
        query.eq(SysUser::getEmail, email);
        SysUser user = this.getOne(query);
        ThrowUtils.throwIf(user == null, ErrorCode.PARAMS_ERROR, "该邮箱未注册");

        // 校验用户状态（使用 Objects.equals 避免装箱问题和NPE）
        ThrowUtils.throwIf(java.util.Objects.equals(user.getUserStatus(), UserStatusEnum.Baned.getValue()),
                ErrorCode.FORBIDDEN_ERROR, "账号已被禁用");

        // 删除验证码
        stringRedisTemplate.delete(codeKey);

        return user;
    }

    @Override
    public boolean logout() {
        // 从当前请求头获取 token
        HttpServletRequest httpRequest = getRequest();
        String token = getTokenFromRequest(httpRequest);
        ThrowUtils.throwIf(StrUtil.isBlank(token), ErrorCode.NOT_LOGIN_ERROR, "未登录");

        // 获取userId（用于删除用户缓存）
        Long userId = UserContext.getCurrentUserId();
        
        // 删除 Redis 中的 token
        String tokenKey = RedisConstants.USER_LOGIN_TOKEN + token;
        Boolean deleted = stringRedisTemplate.delete(tokenKey);
        
        // 删除用户信息缓存
        String userCacheKey = RedisConstants.USER_INFO_CACHE + userId;
        stringRedisTemplate.delete(userCacheKey);

        log.info("用户登出，userId: {}, token：{}", userId, token);
        return deleted;
    }

    @Override
    public boolean resetPassword(SysUserPasswordResetRequest request) {
        String email = request.getEmail();
        String code = request.getCode();
        String newPassword = request.getNewPassword();
        String checkPassword = request.getCheckPassword();

        ThrowUtils.throwIf(StrUtil.isBlank(email), ErrorCode.PARAMS_ERROR, "邮箱不能为空");
        ThrowUtils.throwIf(StrUtil.isBlank(code), ErrorCode.PARAMS_ERROR, "验证码不能为空");
        ThrowUtils.throwIf(StrUtil.isBlank(newPassword), ErrorCode.PARAMS_ERROR, "新密码不能为空");

        // 校验两次密码是否一致
        ThrowUtils.throwIf(!newPassword.equals(checkPassword), ErrorCode.PARAMS_ERROR, "两次密码不一致");

        // 校验验证码
        String codeKey = RedisConstants.LOGIN_EMAIL_CODE + email;
        String cachedCode = stringRedisTemplate.opsForValue().get(codeKey);
        ThrowUtils.throwIf(StrUtil.isBlank(cachedCode), ErrorCode.PARAMS_ERROR, "验证码已过期");
        ThrowUtils.throwIf(!code.equals(cachedCode), ErrorCode.PARAMS_ERROR, "验证码错误");

        // 查询用户
        LambdaQueryWrapper<SysUser> query = new LambdaQueryWrapper<>();
        query.eq(SysUser::getEmail, email);
        SysUser user = this.getOne(query);
        ThrowUtils.throwIf(user == null, ErrorCode.PARAMS_ERROR, "该邮箱未注册");

        // 加密新密码
        String encryptedPassword = passwordEncoder.encode(newPassword);

        // 更新密码
        user.setUserPassword(encryptedPassword);
        boolean updated = this.updateById(user);
        ThrowUtils.throwIf(!updated, ErrorCode.SYSTEM_ERROR, "密码重置失败");

        // 删除验证码
        stringRedisTemplate.delete(codeKey);
        
        // 删除用户缓存（密码已变更）
        String userCacheKey = RedisConstants.USER_INFO_CACHE + user.getId();
        stringRedisTemplate.delete(userCacheKey);

        log.info("用户重置密码成功，邮箱：{}, ID：{}", email, user.getId());
        return true;
    }

    @Override
    public boolean updatePassword(SysUserUpdatePasswordRequest request) {
        String oldPassword = request.getOldPassword();
        String newPassword = request.getNewPassword();
        String checkPassword = request.getCheckPassword();

        ThrowUtils.throwIf(StrUtil.isBlank(oldPassword), ErrorCode.PARAMS_ERROR, "旧密码不能为空");
        ThrowUtils.throwIf(StrUtil.isBlank(newPassword), ErrorCode.PARAMS_ERROR, "新密码不能为空");

        // 校验两次密码是否一致
        ThrowUtils.throwIf(!newPassword.equals(checkPassword), ErrorCode.PARAMS_ERROR, "两次密码不一致");

        // 通过 UserContext 获取当前用户ID（注意：拦截器会清空 userPassword，不能依赖 UserContext.getCurrentUser()）
        Long userId = UserContext.getCurrentUserId();
        SysUser user = this.getById(userId);
        ThrowUtils.throwIf(user == null, ErrorCode.NOT_FOUND_ERROR, "用户不存在");

        // 校验旧密码（DB 中的 userPassword 是 hash）
        boolean matches = passwordEncoder.matches(oldPassword, user.getUserPassword());
        ThrowUtils.throwIf(!matches, ErrorCode.PARAMS_ERROR, "旧密码错误");

        // 加密新密码
        String encryptedPassword = passwordEncoder.encode(newPassword);

        // 更新密码
        user.setUserPassword(encryptedPassword);
        boolean updated = this.updateById(user);
        ThrowUtils.throwIf(!updated, ErrorCode.SYSTEM_ERROR, "密码更新失败");

        // 删除用户缓存（密码已变更）
        String userCacheKey = RedisConstants.USER_INFO_CACHE + userId;
        stringRedisTemplate.delete(userCacheKey);

        log.info("用户修改密码成功，ID：{}", userId);
        return true;
    }

    @Override
    public SysUserVO getCurrentUser() {
        // 通过 UserContext 获取当前用户
        SysUser user = org.zhemu.alterego.util.UserContext.getCurrentUser();
        ThrowUtils.throwIf(user == null, ErrorCode.NOT_FOUND_ERROR, "用户不存在");

        return SysUserVO.objToVo(user);
    }

    /**
     * 从请求头中获取 token
     */
    private String getTokenFromRequest(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (StrUtil.isNotBlank(authHeader) && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return null;
    }

    /**
     * 获取当前 HTTP 请求
     */
    private HttpServletRequest getRequest() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        ThrowUtils.throwIf(attributes == null, ErrorCode.SYSTEM_ERROR, "无法获取请求上下文");
        return attributes.getRequest();
    }
}
