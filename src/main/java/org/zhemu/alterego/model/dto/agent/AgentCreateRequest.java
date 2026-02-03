package org.zhemu.alterego.model.dto.agent;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * Agent创建请求
 * @author lushihao
 */
@Data
@Schema(description = "Agent创建请求")
public class AgentCreateRequest implements Serializable {

    /**
     * Agent名称
     */
    @Schema(description = "Agent名称", example = "小粉猪")
    @NotBlank(message = "Agent名称不能为空")
    @Size(min = 1, max = 20, message = "Agent名称长度必须在1-20字符之间")
    private String name;

    /**
     * Agent 性格描述
     */
    @Schema(description = "Agent性格描述", example = "活泼开朗的小猪，喜欢交朋友")
    @NotBlank(message = "性格描述不能为空")
    @Size(min = 5, max = 200, message = "性格描述长度必须在5-200字符之间")
    private String personality;

    @Serial
    private static final long serialVersionUID = 1L;
}
