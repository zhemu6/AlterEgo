package org.zhemu.alterego.service;

import com.baomidou.mybatisplus.extension.service.IService;
import jakarta.servlet.http.HttpServletRequest;
import org.zhemu.alterego.model.dto.user.SysUserLoginRequest;
import org.zhemu.alterego.model.dto.user.SysUserPasswordResetRequest;
import org.zhemu.alterego.model.dto.user.SysUserRegisterRequest;
import org.zhemu.alterego.model.dto.user.SysUserUpdatePasswordRequest;
import org.zhemu.alterego.model.entity.SysUser;
import org.zhemu.alterego.model.vo.SysUserVO;

/**
 * @author lushihao
 * @description 针对表【sys_user(用户表)】的数据库操作Service
 * @createDate 2026-01-23 23:16:01
 */
public interface SysUserService extends IService<SysUser> {

    /**
     * 用户注册
     *
     * @param request 注册请求
     * @return 新用户 ID
     */
    Long register(SysUserRegisterRequest request);

    /**
     * 用户登录（支持账号密码登录 / 邮箱验证码登录）
     *
     * @param request     登录请求
     * @param httpRequest HTTP 请求
     * @return 用户信息和 token
     */
    SysUserVO login(SysUserLoginRequest request, HttpServletRequest httpRequest);

    /**
     * 用户登出
     *
     * @return 是否成功
     */
    boolean logout();

    /**
     * 重置密码（忘记密码）
     *
     * @param request 重置密码请求
     * @return 是否成功
     */
    boolean resetPassword(SysUserPasswordResetRequest request);

    /**
     * 更新密码（已登录用户修改密码）
     *
     * @param request 更新密码请求
     * @return 是否成功
     */
    boolean updatePassword(SysUserUpdatePasswordRequest request);

    /**
     * 获取当前登录用户
     *
     * @return 当前用户信息
     */
    SysUserVO getCurrentUser();
}

