package org.zhemu.alterego.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.zhemu.alterego.model.dto.agent.AgentCreateRequest;
import org.zhemu.alterego.model.entity.Agent;
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

}
