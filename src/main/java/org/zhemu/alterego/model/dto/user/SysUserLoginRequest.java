package org.zhemu.alterego.model.dto.user;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * 用户登录请求 支持账密登录/邮箱验证码登录
 *
 * @author: lushihao
 * @version: 1.0
 * @create: 2025-07-27 17:00
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SysUserLoginRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 账号
     */
    private String userAccount;

    /**
     * 密码
     */
    private String userPassword;

    /**
     * 邮箱
     */
    private String email;

    /**
     * 验证码
     */
    private String code;

    /**
     * 登录类型：account / email
     */
    @NotBlank(message = "登录方式不能为空")
    private String type;

}
