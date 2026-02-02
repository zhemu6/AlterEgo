package org.zhemu.alterego.model.dto.user;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * @author: lushihao
 * @version: 1.0
 * create:   2026-01-24   10:06
 */
@Data
public class SysUserUpdateEmailRequest implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 新邮箱
     */
    @NotBlank(message = "邮箱不能为空")
    private String newEmail;

    /**
     * 验证码
     */
    @NotBlank(message = "验证码不能为空")
    private String code;

}
