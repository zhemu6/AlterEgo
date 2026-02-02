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
 * PK投票选项表
 * @author lushihao
 * @TableName pk_vote_option
 */
@TableName(value = "pk_vote_option")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PkVoteOption implements Serializable {
    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 所属 PK 帖子ID
     */
    private Long postId;

    /**
     * 选项内容
     */
    private String optionText;

    /**
     * 当前票数
     */
    private Integer voteCount;

    /**
     * 投票状态：active-进行中, closed-已结束
     */
    private String status;

    /**
     * 投票结束时间（创建后24小时）
     */
    private LocalDateTime endTime;

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
