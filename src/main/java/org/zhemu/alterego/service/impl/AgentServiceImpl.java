package org.zhemu.alterego.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.zhemu.alterego.exception.ErrorCode;
import org.zhemu.alterego.exception.ThrowUtils;
import org.zhemu.alterego.mapper.AgentMapper;
import org.zhemu.alterego.model.dto.agent.AgentCreateRequest;
import org.zhemu.alterego.model.entity.Agent;
import org.zhemu.alterego.model.vo.AgentVO;
import org.zhemu.alterego.model.vo.SpeciesVO;
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

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AgentVO createAgent(Long userId, AgentCreateRequest request) {
        log.info("Creating agent for user: {}, name: {}", userId, request.getName());

        // 1. 获取随机物种
        SpeciesVO speciesVO = speciesService.getRandomSpecies();
        ThrowUtils.throwIf(speciesVO == null, ErrorCode.SYSTEM_ERROR, "获取物种失败");

        // 2. 构建Agent对象
        Agent agent = Agent.builder()
                .userId(userId)
                .speciesId(speciesVO.getId())
                // 使用用户自定义名称
                .name(request.getName())
                .personality(request.getPersonality())
                // 初始能量100
                .energy(100)
                .lastEnergyReset(LocalDate.now())
                .postCount(0)
                .commentCount(0)
                // 初始获赞数0
                .likeCount(0)
                // 初始获踩数0
                .dislikeCount(0)
                .createTime(LocalDateTime.now())
                .updateTime(LocalDateTime.now())
                .build();

        // 3. 保存到数据库
        boolean saved = this.save(agent);
        ThrowUtils.throwIf(!saved, ErrorCode.OPERATION_ERROR, "创建Agent失败");

        log.info("Agent created successfully: id={}, name={}, species={}", 
                agent.getId(), agent.getName(), speciesVO.getName());

        // 4. 构建返回VO
        return AgentVO.objToVo(agent, speciesVO);
    }
}
