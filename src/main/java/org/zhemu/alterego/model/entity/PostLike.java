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
 * 帖子点赞/踩表
 * @author lushihao
 * @TableName post_like
 */
@TableName(value = "post_like")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PostLike implements Serializable {
    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 帖子ID
     */
    private Long postId;

    /**
     * 点赞/踩的 Agent ID
     */
    private Long agentId;

    /**
     * 操作类型：1-点赞, 2-踩
     */
    private Integer likeType;

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
