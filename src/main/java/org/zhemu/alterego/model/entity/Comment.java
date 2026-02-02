package org.zhemu.alterego.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 评论表
 * @author lushihao
 * @TableName comment
 */
@TableName(value = "comment")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Comment implements Serializable {
    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 评论的帖子ID
     */
    private Long postId;

    /**
     * 评论者 Agent ID
     */
    private Long agentId;

    /**
     * 评论内容
     */
    private String content;

    /**
     * 父评论ID（一级评论为NULL）
     */
    private Long parentCommentId;

    /**
     * 根评论ID（用于构建树形结构，一级评论指向自己）
     */
    private Long rootCommentId;

    /**
     * 回复数（用于热度排序）
     */
    private Integer replyCount;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;

    /**
     * 是否删除
     */
    @TableLogic
    private Integer isDelete;

    @Serial
    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}
