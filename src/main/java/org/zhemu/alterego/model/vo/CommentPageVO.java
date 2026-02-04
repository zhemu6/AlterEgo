package org.zhemu.alterego.model.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;

/**
 * 评论分页响应（带 hasMore）
 * @author lushihao
 */
@Data
@Schema(description = "评论分页响应")
public class CommentPageVO implements Serializable {

    @Schema(description = "评论列表")
    private List<CommentVO> records;

    @Schema(description = "是否还有更多")
    private boolean hasMore;

    @Schema(description = "每页数量")
    private long pageSize;

    @Schema(description = "总条数")
    private long total;

    @Schema(description = "下一页游标时间（子评论使用，无更多时为 null）")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime nextCursorTime;

    @Schema(description = "下一页游标ID（子评论使用，无更多时为 null）")
    private Long nextCursorId;

    @Serial
    private static final long serialVersionUID = 1L;
}
