package org.zhemu.alterego.service.impl;

import io.agentscope.core.ReActAgent;
import io.agentscope.core.memory.InMemoryMemory;
import io.agentscope.core.memory.autocontext.AutoContextConfig;
import io.agentscope.core.memory.autocontext.AutoContextMemory;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.MsgRole;
import io.agentscope.core.message.TextBlock;
import io.agentscope.core.model.Model;
import io.agentscope.core.session.Session;
import io.agentscope.core.tool.Toolkit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.zhemu.alterego.exception.BusinessException;
import org.zhemu.alterego.exception.ErrorCode;
import org.zhemu.alterego.model.dto.post.AiPostGenerateResult;
import org.zhemu.alterego.model.entity.Agent;
import org.zhemu.alterego.model.entity.Species;
import org.zhemu.alterego.service.AiPostGeneratorService;

import java.util.Collections;

import static org.zhemu.alterego.constant.Constants.AGENT_POST_SESSION_PREFIX;

/**
 * AI 帖子生成服务实现
 * @author lushihao
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AiPostGeneratorServiceImpl implements AiPostGeneratorService {

    private final Model dashScopeModel;

    private final Session mysqlSession;

    @Override
    public AiPostGenerateResult generatePost(Agent agent, Species species) {
        log.info("AI generating post for agent: {}", agent.getName());

        // 这样可以确保每个 Agent 都有自己独立的创作记忆 Session ID 格式: agent_post_{agentId}
        String sessionId = AGENT_POST_SESSION_PREFIX + agent.getId();

        try {
            // 1. 构建 Prompt
            String prompt = String.format("""
                    现在你想发一条社交动态。
                    
                    你的身份：
                    - 物种：%s
                    - 名字：%s
                    - 性格：%s
                    
                    要求：
                    1. 标题要吸引人，符合你的性格。
                    2. 内容要短小精悍（100字以内），像发朋友圈一样。
                    3. 生成 3-5 个有趣的标签（Hashtag）。
                    4. 请回顾你之前的发帖记录（如果有），避免重复相同的话题，保持内容的新鲜感。
                    
                    输出 JSON 格式：
                    {
                      "title": "标题",
                      "content": "内容",
                      "tags": ["标签1", "标签2"]
                    }
                    """, species.getName(), agent.getName(), agent.getPersonality());
            AutoContextConfig autoContextConfig = AutoContextConfig.builder().tokenRatio(0.4).lastKeep(10).build();
            // Use AutoContextMemory, support context auto compression
            AutoContextMemory memory = new AutoContextMemory(autoContextConfig, dashScopeModel);

            // 2. 创建 Agent
            ReActAgent aiAgent = ReActAgent.builder()
                    .name("PostGenerator")
                    .sysPrompt("你是一个擅长角色扮演的 AI，能够完美代入各种角色的性格和说话方式。你有长期的记忆，记得自己之前说过什么。")
                    .model(dashScopeModel)
                    .memory(memory)
                    .maxIters(3)
                    .build();

            // 3. 加载历史记忆（从 MySQL）
            // 这会将之前的发帖记录加载到 Agent 的内存中
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
            Msg response = aiAgent.call(userMsg, AiPostGenerateResult.class).block();

            if (response == null) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "AI 生成无响应");
            }

            // 6. 保存新记忆（到 MySQL）
            // 包含本次的用户 Prompt 和 AI 的 JSON 回复
            try {
                aiAgent.saveTo(mysqlSession, sessionId);
                log.debug("Saved session history for {}", sessionId);
            } catch (Exception e) {
                log.warn("Failed to save session history for {}", sessionId, e);
            }

            // 7. 解析结果
            AiPostGenerateResult result = response.getStructuredData(AiPostGenerateResult.class);
            
            if (result == null || result.title == null || result.content == null) {
                log.warn("AI 生成结果为空或格式错误");
                // 降级方案：返回默认内容
                result = new AiPostGenerateResult();
                result.title = "今天天气真好";
                result.content = "出来晒晒太阳，心情美美哒~";
                result.tags = Collections.singletonList("日常");
            }

            return result;

        } catch (Exception e) {
            log.error("AI 生成帖子失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "AI 生成失败: " + e.getMessage());
        }
    }
}
