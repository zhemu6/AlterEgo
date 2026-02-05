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
import org.zhemu.alterego.exception.ThrowUtils;
import org.zhemu.alterego.model.dto.pk.AiPkVoteResult;
import org.zhemu.alterego.model.entity.Agent;
import org.zhemu.alterego.model.entity.PkVoteOption;
import org.zhemu.alterego.model.entity.Post;
import org.zhemu.alterego.model.entity.Species;
import org.zhemu.alterego.service.AiPkVoteGeneratorService;

import static org.zhemu.alterego.constant.Constants.AGENT_PK_VOTE_SESSION_PREFIX;

/**
 * AI PK 投票生成服务实现
 * @author lushihao
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AiPkVoteGeneratorServiceImpl implements AiPkVoteGeneratorService {

    private final Model dashScopeModel;

    private final Session mysqlSession;

    @Override
    public AiPkVoteResult generateVote(Agent agent, Species species, Post pkPost, PkVoteOption optionA, PkVoteOption optionB) {
        log.info("AI generating vote for agent: {} on PK post: {}", agent.getAgentName(), pkPost.getTitle());

        // 每个 Agent 都有自己独立的 PK 投票记忆 Session ID 格式: agent_pk_vote_{agentId}
        String sessionId = AGENT_PK_VOTE_SESSION_PREFIX + agent.getId();

        try {
            // 1. 构建 Prompt
            String prompt = String.format("""
                现在你要对一个 PK 话题进行投票。
                
                你的身份：
                - 物种：%s
                - 名字：%s
                - 性格：%s
                
                PK 话题：%s
                话题描述：%s
                选项 A：%s
                选项 B：%s
                
                要求：
                1. 你必须选择支持其中一个选项（A 或 B）
                2. 生成 50 字以内的投票理由，符合你的性格
                3. 请回顾你之前的投票记录（如果有），保持你的价值观一致性
                
                输出 JSON 格式：
                {
                  "selectedOption": "A" or "B",
                  "reason": "你的投票理由"
                }
                """, species.getName(), agent.getAgentName(), agent.getPersonality(),
                pkPost.getTitle(), pkPost.getContent(), optionA.getOptionText(), optionB.getOptionText());

            // 2. 配置自动上下文记忆
            AutoContextConfig autoContextConfig = AutoContextConfig.builder().tokenRatio(0.4).lastKeep(10).build();
            AutoContextMemory memory = new AutoContextMemory(autoContextConfig, dashScopeModel);

            // 3. 创建 Agent
            ReActAgent aiAgent = ReActAgent.builder()
                    .name("PkVoteGenerator")
                    .sysPrompt("你是一个擅长角色扮演的 AI，能够完美代入各种角色的性格和价值观。你有长期的记忆，记得自己之前的投票立场。")
                    .model(dashScopeModel)
                    .memory(memory)
                    .maxIters(3)
                    .build();

            // 4. 加载历史记忆（从 MySQL）
            try {
                aiAgent.loadIfExists(mysqlSession, sessionId);
                log.debug("Loaded session history for {}", sessionId);
            } catch (Exception e) {
                log.warn("Failed to load session history for {}, starting fresh.", sessionId, e);
            }

            // 5. 构建消息
            Msg userMsg = Msg.builder()
                    .role(MsgRole.USER)
                    .content(TextBlock.builder().text(prompt).build())
                    .build();

            // 6. 调用 AI
            Msg response = aiAgent.call(userMsg, AiPkVoteResult.class).block();


            ThrowUtils.throwIf(null == response,ErrorCode.SYSTEM_ERROR,"AI 生成无响应");

            // 7. 保存新记忆（到 MySQL）
            try {
                aiAgent.saveTo(mysqlSession, sessionId);
                log.debug("Saved session history for {}", sessionId);
            } catch (Exception e) {
                log.warn("Failed to save session history for {}", sessionId, e);
            }

            // 8. 解析结果
            AiPkVoteResult result = response.getStructuredData(AiPkVoteResult.class);

            // 9. 降级方案
            if (result == null || result.selectedOption == null || result.reason == null) {
                log.warn("AI 生成结果为空或格式错误");
                result = new AiPkVoteResult();
                result.selectedOption = Math.random() > 0.5 ? "A" : "B";
                result.reason = "我觉得这个选项更好！";
            }

            return result;

        } catch (Exception e) {
            log.error("AI 投票生成失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "AI 投票生成失败: " + e.getMessage());
        }
    }
}
