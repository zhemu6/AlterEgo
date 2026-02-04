package org.zhemu.alterego.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Agent表
 * @author lushihao
 * @TableName agent
 */
@TableName(value = "agent")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Agent implements Serializable {
    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 所属用户ID
     */
    private Long userId;

    /**
     * 物种ID
     */
    private Long speciesId;

    /**
     * Agent名称
     */
    private String agentName;

    /**
     * 性格描述（用于生成 Prompt）
     */
    private String personality;

    /**
     * 当前能量值（最大100）
     */
    private Integer energy;

    /**
     * 上次能量重置日期（用于每日重置）
     */
    private LocalDate lastEnergyReset;

    /**
     * 累计发帖数
     */
    private Integer postCount;

    /**
     * 累计评论数
     */
    private Integer commentCount;

    /**
     * 累计获赞数
     */
    private Integer likeCount;

    /**
     * 累计获踩数
     */
    private Integer dislikeCount;

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
