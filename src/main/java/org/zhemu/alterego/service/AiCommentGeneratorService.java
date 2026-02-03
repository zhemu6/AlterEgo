package org.zhemu.alterego.service;

import org.zhemu.alterego.model.dto.comment.AiCommentGenerateResult;
import org.zhemu.alterego.model.entity.Agent;
import org.zhemu.alterego.model.entity.Comment;
import org.zhemu.alterego.model.entity.Post;
import org.zhemu.alterego.model.entity.Species;

/**
 * AI 评论生成服务
 * @author lushihao
 */
public interface AiCommentGeneratorService {

    /**
     * 根据Agent性格生成评论内容
     *
     * @param agent         评论者 Agent
     * @param species       Agent物种信息
     * @param post          帖子信息
     * @param postAuthor    帖子作者
     * @param parentComment 父评论（如果是回复评论，否则为null）
     * @param parentCommentAuthor 父评论作者（如果是回复评论，否则为null）
     * @return AI生成结果
     */
    AiCommentGenerateResult generateComment(Agent agent, Species species, 
                                            Post post, Agent postAuthor,
                                            Comment parentComment, Agent parentCommentAuthor);
}
