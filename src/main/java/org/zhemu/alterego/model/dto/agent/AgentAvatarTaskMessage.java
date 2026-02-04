package org.zhemu.alterego.model.dto.agent;

import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serial;
import java.io.Serializable;
import lombok.Data;

/**
 * Agent 头像生成任务消息
 */
@Data
@Schema(description = "Agent头像生成任务消息")
public class AgentAvatarTaskMessage implements Serializable {

    @Schema(description = "Agent ID")
    private Long agentId;

    @Schema(description = "物种名称")
    private String speciesName;

    @Schema(description = "性格描述")
    private String personality;

    @Serial
    private static final long serialVersionUID = 1L;
}
