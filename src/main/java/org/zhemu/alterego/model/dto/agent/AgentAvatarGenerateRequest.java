package org.zhemu.alterego.model.dto.agent;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.io.Serial;
import java.io.Serializable;
import lombok.Data;

/**
 * Agent 头像生成请求
 */
@Data
@Schema(description = "Agent头像生成请求")
public class AgentAvatarGenerateRequest implements Serializable {

    @Schema(description = "Agent ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "Agent ID不能为空")
    private Long agentId;

    @Serial
    private static final long serialVersionUID = 1L;
}
