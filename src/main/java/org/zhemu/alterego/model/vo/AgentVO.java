package org.zhemu.alterego.model.vo;

import cn.hutool.core.bean.BeanUtil;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.zhemu.alterego.model.entity.Agent;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Agent视图对象（返回给前端）
 * @author lushihao
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Agent视图对象")
public class AgentVO implements Serializable {

    /**
     * 主键ID
     */
    @Schema(description = "Agent ID")
    private Long id;

    /**
     * 所属用户ID
     */
    @Schema(description = "所属用户ID")
    private Long userId;

    /**
     * 物种ID
     */
    @Schema(description = "物种ID")
    private Long speciesId;

    /**
     * Agent名称
     */
    @Schema(description = "Agent名称", example = "小粉猪")
    private String agentName;

    /**
     * Agent头像URL
     */
    @Schema(description = "Agent头像URL")
    private String avatarUrl;

    /**
     * 性格描述
     */
    @Schema(description = "性格描述", example = "活泼开朗的小猪，喜欢交朋友")
    private String personality;

    /**
     * 当前能量值
     */
    @Schema(description = "当前能量值", example = "100")
    private Integer energy;

    /**
     * 上次能量重置日期
     */
    @Schema(description = "上次能量重置日期")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate lastEnergyReset;

    /**
     * 累计发帖数
     */
    @Schema(description = "累计发帖数")
    private Integer postCount;

    /**
     * 累计评论数
     */
    @Schema(description = "累计评论数")
    private Integer commentCount;

    /**
     * 累计获赞数
     */
    @Schema(description = "累计获赞数")
    private Integer likeCount;

    /**
     * 累计获踩数
     */
    @Schema(description = "累计获踩数")
    private Integer dislikeCount;

    /**
     * 创建时间
     */
    @Schema(description = "创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    /**
     * 物种信息（关联查询）
     */
    @Schema(description = "物种信息")
    private SpeciesVO species;

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 对象转VO（不包含关联对象）
     *
     * @param agent Agent实体
     * @return VO对象
     */
    public static AgentVO objToVo(Agent agent) {
        if (agent == null) {
            return null;
        }
        AgentVO agentVO = new AgentVO();
        BeanUtil.copyProperties(agent, agentVO);
        return agentVO;
    }

    /**
     * 对象转VO（包含物种信息）
     *
     * @param agent Agent实体
     * @param speciesVO 物种VO
     * @return VO对象
     */
    public static AgentVO objToVo(Agent agent, SpeciesVO speciesVO) {
        if (agent == null) {
            return null;
        }
        AgentVO agentVO = new AgentVO();
        BeanUtil.copyProperties(agent, agentVO);
        agentVO.species = speciesVO;
        return agentVO;
    }
}
