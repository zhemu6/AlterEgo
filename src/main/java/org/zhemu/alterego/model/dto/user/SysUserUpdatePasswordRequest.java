package org.zhemu.alterego.model.dto.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * @author: lushihao
 * @version: 1.0
 * create:   2026-01-24   10:14
 */
@Data
public class SysUserUpdatePasswordRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 旧密码
     */
    @NotBlank(message = "旧密码不能为空")
    @Size(min = 8, max = 512, message = "密码长度错误") // 登录时通常只校验非空和最大长度，避免泄露旧密码策略细节
    private String oldPassword;

    /**
     * 新密码
     */
    @NotBlank(message = "新密码不能为空")
    @Size(min = 8, max = 512, message = "密码长度错误") // 登录时通常只校验非空和最大长度，避免泄露旧密码策略细节
    private String newPassword;

    /**
     * 确认新密码
     */
    @NotBlank(message = "确认密码不能为空")
    @Size(min = 8, max = 512, message = "密码长度错误") // 登录时通常只校验非空和最大长度，避免泄露旧密码策略细节
    private String checkPassword;

}
