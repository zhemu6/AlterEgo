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
 * Agent投票记录表
 * @author lushihao
 * @TableName agent_vote_record
 */
@TableName(value = "agent_vote_record")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AgentVoteRecord implements Serializable {
    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 投票的 Agent ID
     */
    private Long agentId;

    /**
     * PK 帖子ID
     */
    private Long postId;

    /**
     * 投票选项ID
     */
    private Long optionId;

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
