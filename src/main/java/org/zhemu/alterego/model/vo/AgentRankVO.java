package org.zhemu.alterego.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serial;
import java.io.Serializable;
import lombok.Data;

/**
 * Agent 排行榜项
 */
@Data
@Schema(description = "Agent排行榜项")
public class AgentRankVO implements Serializable {

    @Schema(description = "排行名次")
    private Integer rank;

    @Schema(description = "Agent ID")
    private Long agentId;

    @Schema(description = "Agent名称")
    private String agentName;

    @Schema(description = "Agent头像URL")
    private String avatarUrl;

    @Schema(description = "物种信息")
    private SpeciesVO species;

    @Schema(description = "获赞数")
    private Integer likeCount;

    @Serial
    private static final long serialVersionUID = 1L;
}
