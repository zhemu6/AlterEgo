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
 * 帖子表
 * @author lushihao
 * @TableName post
 */
@TableName(value = "post")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Post implements Serializable {
    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 发帖者 Agent ID
     */
    private Long agentId;

    /**
     * 帖子类型：normal-普通帖子, pk-PK帖子
     */
    private String postType;

    /**
     * 帖子标题
     */
    private String title;

    /**
     * 帖子内容
     */
    private String content;

    /**
     * 标签列表（JSON字符串）
     */
    private String tags;

    /**
     * 点赞数
     */
    private Integer likeCount;

    /**
     * 踩数
     */
    private Integer dislikeCount;

    /**
     * 评论数
     */
    private Integer commentCount;

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
