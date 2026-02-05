package org.zhemu.alterego.service.impl;

import io.agentscope.core.model.Model;
import io.agentscope.core.session.Session;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.zhemu.alterego.model.dto.comment.AiCommentGenerateResult;
import org.zhemu.alterego.model.entity.Agent;
import org.zhemu.alterego.model.entity.Comment;
import org.zhemu.alterego.model.entity.Post;
import org.zhemu.alterego.model.entity.Species;
import org.zhemu.alterego.service.AiCommentGeneratorService;
import org.zhemu.alterego.service.base.AbstractAiTextGenerator;

import static org.zhemu.alterego.constant.Constants.AGENT_COMMENT_SESSION_PREFIX;

/**
 * AI 评论生成服务实现
 * 重构后：继承 AbstractAiTextGenerator
 * @author lushihao
 */
@Service
@Slf4j
public class AiCommentGeneratorServiceImpl 
    extends AbstractAiTextGenerator<AiCommentGeneratorServiceImpl.CommentRequest, AiCommentGenerateResult> 
    implements AiCommentGeneratorService {

    @Data
    @AllArgsConstructor
    public static class CommentRequest {
        private Post post;
        private Agent postAuthor;
        private Comment parentComment;
        private Agent parentCommentAuthor;
    }

    public AiCommentGeneratorServiceImpl(Model dashScopeModel, Session mysqlSession) {
        super(dashScopeModel, mysqlSession);
    }

    @Override
    protected String getSessionPrefix() {
        return AGENT_COMMENT_SESSION_PREFIX;
    }

    @Override
    protected String getGeneratorType() {
        return "comment";
    }

    @Override
    protected String buildSessionId(Agent agent, CommentRequest request) {
        return getSessionPrefix() + agent.getId() + "_" + request.getPost().getId();
    }

    @Override
    protected String buildPrompt(Agent agent, Species species, CommentRequest request) {
        Post post = request.getPost();
        Agent postAuthor = request.getPostAuthor();
        Comment parentComment = request.getParentComment();
        Agent parentCommentAuthor = request.getParentCommentAuthor();
        
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
                species.getName(), agent.getAgentName(), agent.getPersonality(),
                post.getTitle(), post.getContent(), postAuthor.getAgentName()));

        if (parentComment != null) {
            sb.append(String.format("""
                    
                    你要回复的评论：
                    - 评论者：%s
                    - 评论内容：%s
                    
                    要求：
                    1. 根据你的性格，回复这条评论（50字以内）。
                    2. 可以赞同、反驳、补充、调侃等，符合你的性格。
                    3. 决定是否点赞（like）或踩（dislike）这条评论（注意是评论，不是帖子）。
                    4. 回顾你之前的评论记录，避免重复。
                    """,
                    parentCommentAuthor != null ? parentCommentAuthor.getAgentName() : "未知用户",
                    parentComment.getContent()));
        } else {
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
                  "like": true/false, // 是否点赞（对帖子或评论）
                  "dislike": true/false // 是否点踩（对帖子或评论）
                }
                
                注意：like 和 dislike 不能同时为 true。并且必须表达你的态度。
                """);

        return sb.toString();
    }

    @Override
    protected Class<AiCommentGenerateResult> getResultClass() {
        return AiCommentGenerateResult.class;
    }

    @Override
    protected AiCommentGenerateResult getFallbackResult() {
        AiCommentGenerateResult result = new AiCommentGenerateResult();
        result.setContent("有点意思。");
        result.setLike(true);
        return result;
    }

    @Override
    protected boolean validateResult(AiCommentGenerateResult result) {
        return result != null && result.getContent() != null;
    }

    @Override
    protected String getAgentName() {
        return "CommentGenerator";
    }

    @Override
    protected String getSystemPrompt() {
        return "你是一个擅长社交互动的 AI，能够根据人设对帖子发表看法。你有记忆，记得之前的观点。";
    }

    @Override
    public AiCommentGenerateResult generateComment(Agent agent, Species species, 
                                                   Post post, Agent postAuthor,
                                                   Comment parentComment, Agent parentCommentAuthor) {
        CommentRequest request = new CommentRequest(post, postAuthor, parentComment, parentCommentAuthor);
        return generate(agent, species, request);
    }
}
