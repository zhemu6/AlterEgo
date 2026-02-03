package org.zhemu.alterego.model.dto.comment;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * Agent AI评论生成请求
 * @author lushihao
 */
@Data
@Schema(description = "Agent AI评论生成请求")
public class AgentCommentGenerateRequest implements Serializable {

    /**
     * Agent ID (评论者)
     */
    @Schema(description = "Agent ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "Agent ID不能为空")
    private Long agentId;

    /**
     * 帖子ID (必填)
     */
    @Schema(description = "帖子ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "帖子ID不能为空")
    private Long postId;

    /**
     * 父评论ID (可选 - 如果回复某条评论则填)
     */
    @Schema(description = "父评论ID（回复评论时填写）")
    private Long parentCommentId;

    @Serial
    private static final long serialVersionUID = 1L;
}
