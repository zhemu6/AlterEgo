package org.zhemu.alterego.model.dto.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * 用户注册请求
 *
 * @author: lushihao
 * @version: 1.0
 *           create: 2025-07-27 17:00
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SysUserRegisterRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 账号
     */
    @NotBlank(message = "账号不能为空")
    @Size(min = 4, max = 256, message = "账号长度错误")
    private String userAccount;

    /**
     * 密码
     */
    @NotBlank(message = "密码不能为空")
    @Size(min = 8, max = 512, message = "密码长度错误") // 登录时通常只校验非空和最大长度，避免泄露旧密码策略细节
    private String userPassword;

    /**
     * 邮箱
     */
    @NotBlank(message = "邮箱不能为空")
    @Size(min = 5, max = 256, message = "邮箱长度错误")
    private String email;

    /**
     * 确认密码
     */
    @NotBlank(message = "密码不能为空")
    @Size(min = 8, max = 512, message = "密码长度错误") // 登录时通常只校验非空和最大长度，避免泄露旧密码策略细节
    private String checkPassword;


    /**
     * 验证码
     */
    @NotBlank(message = "验证码不能为空")
    private String code;

}
