package org.zhemu.alterego.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.zhemu.alterego.exception.ErrorCode;
import org.zhemu.alterego.exception.ThrowUtils;
import org.zhemu.alterego.mapper.AgentMapper;
import org.zhemu.alterego.model.dto.agent.AgentAvatarTaskMessage;
import org.zhemu.alterego.model.dto.agent.AgentCreateRequest;
import org.zhemu.alterego.model.entity.Agent;
import org.zhemu.alterego.model.vo.AgentVO;
import org.zhemu.alterego.model.vo.SpeciesVO;
import org.zhemu.alterego.mq.MessageProducer;
import org.zhemu.alterego.service.AgentService;
import org.zhemu.alterego.service.SpeciesService;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * @author lushihao
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AgentServiceImpl extends ServiceImpl<AgentMapper, Agent>
        implements AgentService {

    private final SpeciesService speciesService;
    private final MessageProducer messageProducer;

    @Value("${agent.avatar.default-url:}")
    private String defaultAvatarUrl;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AgentVO createAgent(Long userId, AgentCreateRequest request) {
        log.info("Creating agent for user: {}, name: {}", userId, request.getAgentName());

        // 1. 获取随机物种
        SpeciesVO speciesVO = speciesService.getRandomSpecies();
        ThrowUtils.throwIf(speciesVO == null, ErrorCode.SYSTEM_ERROR, "获取物种失败");

        // 2. 构建Agent对象
        Agent agent = Agent.builder()
                .userId(userId)
                .speciesId(speciesVO.getId())
                // 使用用户自定义名
                .agentName(request.getAgentName())
                .avatarUrl(defaultAvatarUrl)
                .personality(request.getPersonality())
                // 初始能量100
                .energy(100)
                .lastEnergyReset(LocalDate.now())
                .postCount(0)
                .commentCount(0)
                // 初始获赞
                .likeCount(0)
                // 初始获踩
                .dislikeCount(0)
                .createTime(LocalDateTime.now())
                .updateTime(LocalDateTime.now())
                .build();

        // 3. 保存到数据库
        boolean saved = this.save(agent);
        ThrowUtils.throwIf(!saved, ErrorCode.OPERATION_ERROR, "创建Agent失败");

        log.info("Agent created successfully: id={}, name={}, species={}",
                agent.getId(), agent.getAgentName(), speciesVO.getName());

        // 4. 构建返回VO
        return AgentVO.objToVo(agent, speciesVO);
    }

    @Override
    public AgentVO getAgentByUserId(Long userId) {
        // 1. 查询Agent
        LambdaQueryWrapper<Agent> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Agent::getUserId, userId);
        Agent agent = this.getOne(queryWrapper);

        if (agent == null) {
            return null;
        }

        // 2. 获取物种信息
        SpeciesVO speciesVO = speciesService.getSpeciesById(agent.getSpeciesId());
        
        // 3. 封装VO
        return AgentVO.objToVo(agent, speciesVO);
    }

    @Override
    public boolean generateAvatar(Long userId, Long agentId) {
        ThrowUtils.throwIf(agentId == null || agentId <= 0, ErrorCode.PARAMS_ERROR, "Agent ID不合法");
        Agent agent = this.getById(agentId);
        ThrowUtils.throwIf(agent == null, ErrorCode.NOT_FOUND_ERROR, "Agent不存在");
        ThrowUtils.throwIf(!agent.getUserId().equals(userId), ErrorCode.NO_AUTH_ERROR, "无权限操作该Agent");
        SpeciesVO speciesVO = speciesService.getSpeciesById(agent.getSpeciesId());
        ThrowUtils.throwIf(speciesVO == null, ErrorCode.SYSTEM_ERROR, "获取物种失败");

        AgentAvatarTaskMessage task = new AgentAvatarTaskMessage();
        task.setAgentId(agent.getId());
        task.setSpeciesName(speciesVO.getName());
        task.setPersonality(agent.getPersonality());
        messageProducer.sendAgentAvatarTask(task);
        return true;
    }
}
