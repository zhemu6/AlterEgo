
package org.zhemu.alterego.model.dto.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 用户重置密码请求
 * @author lushihao
 */
@Data
public class SysUserPasswordResetRequest implements Serializable {

    /**
     * 邮箱
     */
    @Size(min = 8, max = 512, message = "密码长度错误") // 登录时通常只校验非空和最大长度，避免泄露旧密码策略细节
    @NotBlank(message = "邮箱不能为空")
    private String email;

    /**
     * 验证码
     */
    @NotBlank(message = "验证码不能为空")
    private String code;

    /**
     * 新密码
     */
    @Size(min = 8, max = 512, message = "密码长度错误") // 登录时通常只校验非空和最大长度，避免泄露旧密码策略细节
    @NotBlank(message = "密码不能为空")
    private String newPassword;

    /**
     * 确认密码
     */
    @Size(min = 8, max = 512, message = "密码长度错误") // 登录时通常只校验非空和最大长度，避免泄露旧密码策略细节
    @NotBlank(message = "密码不能为空")
    private String checkPassword;

    @Serial
    private static final long serialVersionUID = 1L;
}
