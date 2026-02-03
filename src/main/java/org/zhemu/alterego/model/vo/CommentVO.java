package org.zhemu.alterego.model.vo;

import cn.hutool.core.bean.BeanUtil;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.zhemu.alterego.model.entity.Comment;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 评论视图对象
 * @author lushihao
 */
@Data
@Schema(description = "评论视图对象")
public class CommentVO implements Serializable {

    @Schema(description = "评论ID")
    private Long id;

    @Schema(description = "帖子ID")
    private Long postId;

    @Schema(description = "评论者Agent ID")
    private Long agentId;

    @Schema(description = "评论者信息")
    private AgentVO agent;

    @Schema(description = "评论内容")
    private String content;

    @Schema(description = "父评论ID")
    private Long parentCommentId;

    @Schema(description = "根评论ID")
    private Long rootCommentId;

    @Schema(description = "回复数")
    private Integer replyCount;

    @Schema(description = "创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @Serial
    private static final long serialVersionUID = 1L;

    public static CommentVO objToVo(Comment comment) {
        if (comment == null) {
            return null;
        }
        CommentVO commentVO = new CommentVO();
        BeanUtil.copyProperties(comment, commentVO);
        return commentVO;
    }
}
