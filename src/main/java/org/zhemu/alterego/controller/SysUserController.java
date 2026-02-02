package org.zhemu.alterego.controller;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.zhemu.alterego.annotation.RequireLogin;
import org.zhemu.alterego.common.BaseResponse;
import org.zhemu.alterego.common.ResultUtils;
import org.zhemu.alterego.exception.ErrorCode;
import org.zhemu.alterego.exception.ThrowUtils;
import org.zhemu.alterego.model.dto.user.SysUserLoginRequest;
import org.zhemu.alterego.model.dto.user.SysUserPasswordResetRequest;
import org.zhemu.alterego.model.dto.user.SysUserRegisterRequest;
import org.zhemu.alterego.model.dto.user.SysUserUpdatePasswordRequest;
import org.zhemu.alterego.model.vo.SysUserVO;
import org.zhemu.alterego.service.MailService;
import org.zhemu.alterego.service.SysUserService;

import java.util.List;

/**
 * 用户控制器
 *
 * @author: lushihao
 * @version: 1.0
 * create: 2026-01-23 23:40
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/user")
@Slf4j
@Tag(name = "用户模块", description = "用户注册、登录、密码管理等接口")
public class SysUserController {

    private final SysUserService userService;
    private final MailService mailService;
    private final StringRedisTemplate stringRedisTemplate;

    /**
     * 发送邮箱验证码
     */
    @PostMapping("/send-code")
    @Operation(summary = "发送邮箱验证码", description = "用于注册、登录、重置密码")
    public BaseResponse<String> sendCode(@RequestParam String email) {
        ThrowUtils.throwIf(null == email || email.isBlank(), ErrorCode.PARAMS_ERROR, "邮箱不能为空");
        mailService.sendCode(email);
        return ResultUtils.success("验证码已发送");
    }

    /**
     * 用户注册
     */
    @PostMapping("/register")
    @Operation(summary = "用户注册", description = "通过邮箱验证码注册账号")
    public BaseResponse<Long> register(@RequestBody @Validated SysUserRegisterRequest request) {
        Long userId = userService.register(request);
        return ResultUtils.success(userId);
    }

    /**
     * 用户登录
     */
    @PostMapping("/login")
    @Operation(summary = "用户登录", description = "支持账号密码登录和邮箱验证码登录")
    public BaseResponse<SysUserVO> login(
            @RequestBody @Validated SysUserLoginRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {
        SysUserVO userVO = userService.login(request, httpRequest);

        // 从 request attribute 获取 token 并设置到响应头
        String token = (String) httpRequest.getAttribute("token");
        if (token != null) {
            httpResponse.setHeader("Authorization", "Bearer " + token);
        }

        return ResultUtils.success(userVO);
    }

    /**
     * 用户登出
     */
    @PostMapping("/logout")
    @RequireLogin
    @Operation(summary = "用户登出", description = "清除登录状态")
    public BaseResponse<Boolean> logout() {
        boolean result = userService.logout();
        return ResultUtils.success(result);
    }

    /**
     * 重置密码（忘记密码）
     */
    @PostMapping("/password/reset")
    @Operation(summary = "重置密码", description = "通过邮箱验证码重置密码")
    public BaseResponse<Boolean> resetPassword(@RequestBody @Validated SysUserPasswordResetRequest request) {
        boolean result = userService.resetPassword(request);
        return ResultUtils.success(result);
    }

    /**
     * 修改密码（用户中心）
     */
    @PutMapping("/password")
    @RequireLogin
    @Operation(summary = "修改密码", description = "已登录用户修改密码")
    public BaseResponse<Boolean> updatePassword(@RequestBody @Validated SysUserUpdatePasswordRequest request) {
        boolean result = userService.updatePassword(request);
        return ResultUtils.success(result);
    }

    /**
     * 获取当前登录用户
     */
    @GetMapping("/current")
    @RequireLogin
    @Operation(summary = "获取当前用户", description = "获取当前登录用户信息")
    public BaseResponse<SysUserVO> getCurrentUser() {
        SysUserVO userVO = userService.getCurrentUser();
        return ResultUtils.success(userVO);
    }
}

