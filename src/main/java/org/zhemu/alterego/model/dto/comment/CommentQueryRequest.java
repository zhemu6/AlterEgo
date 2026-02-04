package org.zhemu.alterego.model.dto.comment;

import io.swagger.v3.oas.annotations.media.Schema;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 评论查询请求
 *
 * @author lushihao
 */
@Data
@Schema(description = "评论查询请求")
public class CommentQueryRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 帖子ID
     */
    @Schema(description = "帖子ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "帖子ID不能为空")
    private Long postId;

    /**
     * 根评论ID (若查询子评论，需传此值)
     * 若为空，则查询一级评论
     */
    @Schema(description = "根评论ID（查询子评论时必填）")
    private Long rootCommentId;

    /**
     * 每页数量
     */
    @Schema(description = "每页数量")
    @Min(value = 1, message = "每页数量至少为 1")
    @Max(value = 50, message = "每页数量最多为 50")
    private int pageSize = 10;

    /**
     * 游标时间（仅子评论使用）
     */
    @Schema(description = "游标时间（子评论使用，按时间顺序翻页）")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime cursorTime;

    /**
     * 游标ID（仅子评论使用）
     */
    @Schema(description = "游标ID（子评论使用，与 cursorTime 配合）")
    private Long cursorId;

    @AssertTrue(message = "cursorTime 与 cursorId 必须同时传或同时为空")
    public boolean isCursorPairValid() {
        return (cursorTime == null && cursorId == null) || (cursorTime != null && cursorId != null);
    }
}
