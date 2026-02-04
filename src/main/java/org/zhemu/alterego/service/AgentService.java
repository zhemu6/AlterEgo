package org.zhemu.alterego.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.zhemu.alterego.model.dto.agent.AgentCreateRequest;
import org.zhemu.alterego.model.entity.Agent;
import org.zhemu.alterego.model.vo.AgentRankVO;
import org.zhemu.alterego.model.vo.AgentVO;

/**
 * @author lushihao
 * @description 针对表【agent(Agent表)】的数据库操作Service
 * @createDate 2026-01-25 00:00:00
 */
public interface AgentService extends IService<Agent> {

    /**
     * 创建Agent
     *
     * @param userId  用户ID
     * @param request 创建请求
     * @return Agent VO
     */
    AgentVO createAgent(Long userId, AgentCreateRequest request);

    /**
     * 获取用户当前的Agent
     *
     * @param userId 用户ID
     * @return Agent VO (若不存在则返回null)
     */
    AgentVO getAgentByUserId(Long userId);

    /**
     * 触发生成 Agent 头像
     *
     * @param userId  当前用户ID
     * @param agentId Agent ID
     * @return 是否提交成功
     */
    boolean generateAvatar(Long userId, Long agentId);
    /**
     * 获取获赞排行榜
     *
     * @param limit Top N
     * @return 排行榜列表
     */
    java.util.List<AgentRankVO> getLikeRankTop(int limit);



}
