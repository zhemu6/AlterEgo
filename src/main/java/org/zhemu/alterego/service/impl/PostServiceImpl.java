package org.zhemu.alterego.service.impl;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.zhemu.alterego.exception.BusinessException;
import org.zhemu.alterego.exception.ErrorCode;
import org.zhemu.alterego.exception.ThrowUtils;
import org.zhemu.alterego.mapper.PostMapper;
import org.zhemu.alterego.model.dto.post.AgentPostGenerateRequest;
import org.zhemu.alterego.model.dto.post.AiPostGenerateResult;
import org.zhemu.alterego.model.entity.Agent;
import org.zhemu.alterego.model.entity.Post;
import org.zhemu.alterego.model.entity.Species;
import org.zhemu.alterego.model.vo.PostVO;
import org.zhemu.alterego.service.AgentService;
import org.zhemu.alterego.service.AiPostGeneratorService;
import org.zhemu.alterego.service.PostService;
import org.zhemu.alterego.service.SpeciesService;

import java.time.LocalDateTime;

/**
 * @author lushihao
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PostServiceImpl extends ServiceImpl<PostMapper, Post>
        implements PostService {

    private final AgentService agentService;
    private final SpeciesService speciesService;
    private final AiPostGeneratorService aiPostGeneratorService;

    // 发帖消耗能量
    private static final int POST_ENERGY_COST = 10;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PostVO aiGeneratePost(AgentPostGenerateRequest request, Long userId) {
        Long agentId = request.getAgentId();
        
        // 1. 获取并校验 Agent
        Agent agent = agentService.getById(agentId);
        ThrowUtils.throwIf(agent == null, ErrorCode.NOT_FOUND_ERROR, "Agent不存在");
        
        // 2. 校验归属权
        ThrowUtils.throwIf(!agent.getUserId().equals(userId), ErrorCode.NO_AUTH_ERROR, "只能操作自己的Agent");
        
        // 3. 校验能量
        ThrowUtils.throwIf(agent.getEnergy() < POST_ENERGY_COST, ErrorCode.OPERATION_ERROR, "能量不足，无法发帖（需要 " + POST_ENERGY_COST + " 点）");
        
        // 4. 获取物种信息（用于 AI 生成）
        Species species = speciesService.getById(agent.getSpeciesId());
        
        // 5. 调用 AI 生成内容
        AiPostGenerateResult aiResult = aiPostGeneratorService.generatePost(agent, species);
        
        // 6. 保存帖子
        Post post = Post.builder()
                .agentId(agentId)
                .title(aiResult.title)
                .content(aiResult.content)
                .type("normal")
                // 转换为 JSON 字符串存储
                .tags(JSONUtil.toJsonStr(aiResult.tags))
                .likeCount(0)
                .dislikeCount(0)
                .commentCount(0)
                .createTime(LocalDateTime.now())
                .updateTime(LocalDateTime.now())
                .isDelete(0)
                .build();
        
        boolean saved = this.save(post);
        ThrowUtils.throwIf(!saved, ErrorCode.OPERATION_ERROR, "发帖失败");
        
        // 7. 扣除能量 & 更新统计
        agent.setEnergy(agent.getEnergy() - POST_ENERGY_COST);
        agent.setPostCount(agent.getPostCount() + 1);
        boolean agentUpdated = agentService.updateById(agent);
        ThrowUtils.throwIf(!agentUpdated, ErrorCode.OPERATION_ERROR, "更新Agent状态失败");
        
        log.info("Agent {} post generated successfully: {}", agentId, post.getId());
        
        return PostVO.objToVo(post);
    }
}
