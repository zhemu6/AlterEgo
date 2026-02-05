package org.zhemu.alterego.service.impl;

import io.agentscope.core.ReActAgent;
import io.agentscope.core.memory.autocontext.AutoContextConfig;
import io.agentscope.core.memory.autocontext.AutoContextMemory;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.MsgRole;
import io.agentscope.core.message.TextBlock;
import io.agentscope.core.model.Model;
import io.agentscope.core.session.Session;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.zhemu.alterego.exception.BusinessException;
import org.zhemu.alterego.exception.ErrorCode;
import org.zhemu.alterego.model.dto.pk.AiPkGenerateResult;
import org.zhemu.alterego.model.entity.Agent;
import org.zhemu.alterego.model.entity.Species;
import org.zhemu.alterego.service.AiPkGeneratorService;

import java.util.Collections;

import static org.zhemu.alterego.constant.Constants.AGENT_PK_SESSION_PREFIX;

/**
 * AI PK 话题生成服务实现
 * @author lushihao
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AiPkGeneratorServiceImpl implements AiPkGeneratorService {

    private final Model dashScopeModel;

    private final Session mysqlSession;

    @Override
    public AiPkGenerateResult generatePk(Agent agent, Species species) {
        log.info("AI generating PK topic for agent: {}", agent.getAgentName());

        // 每个 Agent 都有自己独立的 PK 创作记忆 Session ID 格式: agent_pk_{agentId}
        String sessionId = AGENT_PK_SESSION_PREFIX + agent.getId();

        try {
            // 1. 构建 Prompt
            String prompt = String.format("""
                    现在你想发起一个有趣的 PK 投票话题。
                    
                    你的身份：
                    - 物种：%s
                    - 名字：%s
                    - 性格：%s
                    
                    要求：
                    1. 话题要有趣、有争议性
                    2. 两个选项要对立但都有道理
                    3. 选项文字简短（10字以内），可用emoji
                    4. 请回顾你之前的 PK 话题（如果有），避免重复相同主题
                    
                    输出 JSON 格式：
                    {
                      "topic": "话题标题（30字以内）",
                      "description": "话题描述（100字以内）",
                      "optionA": "选项A文字",
                      "optionB": "选项B文字",
                      "tags": ["标签1", "标签2"]
                    }
                    """, species.getName(), agent.getAgentName(), agent.getPersonality());

            AutoContextConfig autoContextConfig = AutoContextConfig.builder().tokenRatio(0.4).lastKeep(10).build();
            // Use AutoContextMemory, support context auto compression
            AutoContextMemory memory = new AutoContextMemory(autoContextConfig, dashScopeModel);

            // 2. 创建 Agent
            ReActAgent aiAgent = ReActAgent.builder()
                    .name("PkGenerator")
                    .sysPrompt("你是一个擅长角色扮演的 AI，能够完美代入各种角色的性格和说话方式。你有长期的记忆，记得自己之前发起过什么话题。")
                    .model(dashScopeModel)
                    .memory(memory)
                    .maxIters(3)
                    .build();

            // 3. 加载历史记忆（从 MySQL）
            try {
                aiAgent.loadIfExists(mysqlSession, sessionId);
                log.debug("Loaded session history for {}", sessionId);
            } catch (Exception e) {
                log.warn("Failed to load session history for {}, starting fresh.", sessionId, e);
            }

            // 4. 构建消息
            Msg userMsg = Msg.builder()
                    .role(MsgRole.USER)
                    .content(TextBlock.builder().text(prompt).build())
                    .build();

            // 5. 调用 AI
            Msg response = aiAgent.call(userMsg, AiPkGenerateResult.class).block();

            if (response == null) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "AI 生成无响应");
            }

            // 6. 保存新记忆（到 MySQL）
            try {
                aiAgent.saveTo(mysqlSession, sessionId);
                log.debug("Saved session history for {}", sessionId);
            } catch (Exception e) {
                log.warn("Failed to save session history for {}", sessionId, e);
            }

            // 7. 解析结果
            AiPkGenerateResult result = response.getStructuredData(AiPkGenerateResult.class);

            if (result == null || result.topic == null || result.description == null) {
                log.warn("AI 生成结果为空或格式错误");
                // 降级方案：返回默认内容
                result = new AiPkGenerateResult();
                result.topic = "猫派 vs 狗派";
                result.description = "你更喜欢猫还是狗？";
                result.optionA = "猫咪派🐱";
                result.optionB = "狗狗派🐶";
                result.tags = Collections.singletonList("宠物");
            }

            return result;

        } catch (Exception e) {
            log.error("AI 生成帖子失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "AI 生成失败: " + e.getMessage());
        }
    }
}
