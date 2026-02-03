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
import org.zhemu.alterego.constant.Constants;
import org.zhemu.alterego.exception.BusinessException;
import org.zhemu.alterego.exception.ErrorCode;
import org.zhemu.alterego.model.dto.comment.AiCommentGenerateResult;
import org.zhemu.alterego.model.entity.Agent;
import org.zhemu.alterego.model.entity.Comment;
import org.zhemu.alterego.model.entity.Post;
import org.zhemu.alterego.model.entity.Species;
import org.zhemu.alterego.service.AiCommentGeneratorService;

/**
 * AI 评论生成服务实现
 * @author lushihao
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AiCommentGeneratorServiceImpl implements AiCommentGeneratorService {

    private final Model dashScopeModel;
    private final Session mysqlSession;

    @Override
    public AiCommentGenerateResult generateComment(Agent agent, Species species, 
                                                   Post post, Agent postAuthor,
                                                   Comment parentComment, Agent parentCommentAuthor) {
        log.info("AI generating comment for agent: {}, target post: {}", agent.getName(), post.getId());

        // Session ID: agent_comment_{agentId}_{postId}
        // 记忆粒度：每个 Agent 在每个帖子下的互动记忆
        String sessionId = Constants.AGENT_COMMENT_SESSION_PREFIX + agent.getId() + "_" + post.getId();
        AutoContextConfig autoContextConfig = AutoContextConfig.builder().tokenRatio(0.4).lastKeep(10).build();
        // Use AutoContextMemory, support context auto compression
        AutoContextMemory memory = new AutoContextMemory(autoContextConfig, dashScopeModel);
        try {
            // 1. 构建 Prompt
            String prompt = buildPrompt(agent, species, post, postAuthor, parentComment, parentCommentAuthor);

            // 2. 创建 Agent
            ReActAgent aiAgent = ReActAgent.builder()
                    .name("CommentGenerator")
                    .sysPrompt("你是一个擅长社交互动的 AI，能够根据人设对帖子发表看法。你有记忆，记得之前的观点。")
                    .model(dashScopeModel)
                    .memory(memory)
                    .maxIters(3)
                    .build();

            // 3. 加载历史记忆
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
            Msg response = aiAgent.call(userMsg, AiCommentGenerateResult.class).block();

            if (response == null) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "AI 生成无响应");
            }

            // 6. 保存新记忆
            try {
                aiAgent.saveTo(mysqlSession, sessionId);
                log.debug("Saved session history for {}", sessionId);
            } catch (Exception e) {
                log.warn("Failed to save session history for {}", sessionId, e);
            }

            // 7. 解析结果
            AiCommentGenerateResult result = response.getStructuredData(AiCommentGenerateResult.class);
            
            // 兜底逻辑
            if (result == null || result.getContent() == null) {
                log.warn("AI 生成结果为空或格式错误");
                result = new AiCommentGenerateResult();
                result.setContent("有点意思。");
                result.setLike(true);
            }

            return result;

        } catch (Exception e) {
            log.error("AI 生成评论失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "AI 生成失败: " + e.getMessage());
        }
    }

    private String buildPrompt(Agent agent, Species species, Post post, Agent postAuthor, 
                               Comment parentComment, Agent parentCommentAuthor) {
        StringBuilder sb = new StringBuilder();
        
        sb.append(String.format("""
                你的身份：
                - 物种：%s
                - 名字：%s
                - 性格：%s
                
                帖子信息：
                - 标题：%s
                - 内容：%s
                - 作者：%s
                """, 
                species.getName(), agent.getName(), agent.getPersonality(),
                post.getTitle(), post.getContent(), postAuthor.getName()));

        if (parentComment != null) {
            // 父评论非空 回复评论场景
            sb.append(String.format("""
                    
                    你要回复的评论：
                    - 评论者：%s
                    - 评论内容：%s
                    
                    要求：
                    1. 根据你的性格，回复这条评论（50字以内）。
                    2. 可以赞同、反驳、补充、调侃等，符合你的性格。
                    3. 决定是否点赞（like）或踩（dislike）这篇帖子（注意是帖子，不是评论）。
                    4. 回顾你之前的评论记录，避免重复。
                    """, 
                    parentCommentAuthor != null ? parentCommentAuthor.getName() : "未知用户",
                    parentComment.getContent()));
        } else {
            // 父评论为空 回复评论帖子场景
            sb.append("""
                    
                    要求：
                    1. 根据你的性格，对这篇帖子发表评论（50字以内）。
                    2. 可以表达支持、质疑、讽刺、补充信息等，符合你的性格。
                    3. 决定是否点赞（like）或踩（dislike）这篇帖子。
                    4. 回顾你之前的评论记录，避免重复。
                    """);
        }

        sb.append("""
                
                输出 JSON 格式：
                {
                  "content": "你的评论/回复内容",
                  "like": true/false, // 是否点赞帖子
                  "dislike": true/false // 是否踩帖子
                }
                
                注意：like 和 dislike 不能同时为 true。如果无感，都设为 false 或 null。
                """);

        return sb.toString();
    }
}
