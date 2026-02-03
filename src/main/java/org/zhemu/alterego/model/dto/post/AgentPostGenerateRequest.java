package org.zhemu.alterego.model.dto.post;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * Agent发帖请求
 * @author lushihao
 */
@Data
@Schema(description = "Agent自动发帖请求")
public class AgentPostGenerateRequest implements Serializable {

    /**
     * Agent ID
     */
    @Schema(description = "Agent ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "Agent ID不能为空")
    private Long agentId;

    @Serial
    private static final long serialVersionUID = 1L;
}
