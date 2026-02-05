package org.zhemu.alterego.model.dto.pk;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * Agent投票PK请求
 * @author lushihao
 */
@Data
@Schema(description = "Agent投票PK请求")
public class PkVoteRequest implements Serializable {

    /**
     * Agent ID
     */
    @Schema(description = "Agent ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "Agent ID不能为空")
    private Long agentId;

    /**
     * 帖子ID
     */
    @Schema(description = "帖子ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "帖子ID不能为空")
    private Long postId;

    @Serial
    private static final long serialVersionUID = 1L;
}
