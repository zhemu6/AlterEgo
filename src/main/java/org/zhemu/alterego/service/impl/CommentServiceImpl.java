package org.zhemu.alterego.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.zhemu.alterego.exception.ErrorCode;
import org.zhemu.alterego.exception.ThrowUtils;
import org.zhemu.alterego.mapper.CommentMapper;
import org.zhemu.alterego.model.dto.comment.AgentCommentGenerateRequest;
import org.zhemu.alterego.model.dto.comment.AiCommentGenerateResult;
import org.zhemu.alterego.model.entity.*;
import org.zhemu.alterego.model.vo.CommentVO;
import org.zhemu.alterego.service.*;

import java.time.LocalDateTime;

/**
 * @author lushihao
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CommentServiceImpl extends ServiceImpl<CommentMapper, Comment>
        implements CommentService {

    private final AgentService agentService;
    private final SpeciesService speciesService;
    private final PostService postService;
    private final PostLikeService postLikeService;
    private final AiCommentGeneratorService aiCommentGeneratorService;

    // 评论消耗能量
    private static final int COMMENT_ENERGY_COST = 5;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CommentVO aiGenerateComment(AgentCommentGenerateRequest request, Long userId) {
        // 评论agent Id
        Long agentId = request.getAgentId();
        // 帖子 id
        Long postId = request.getPostId();
        // 父评论 id 可能为空
        Long parentCommentId = request.getParentCommentId();

        // ========== 1. 校验阶段 ==========
        
        // 1.1 获取并校验 Agent
        Agent agent = agentService.getById(agentId);
        ThrowUtils.throwIf(agent == null, ErrorCode.NOT_FOUND_ERROR, "Agent不存在");

        // 1.2 校验归属权
        ThrowUtils.throwIf(!agent.getUserId().equals(userId), 
                           ErrorCode.NO_AUTH_ERROR, "只能操作自己的Agent");

        // 1.3 校验能量
        ThrowUtils.throwIf(agent.getEnergy() < COMMENT_ENERGY_COST, 
                           ErrorCode.OPERATION_ERROR, 
                           "能量不足，无法评论（需要 " + COMMENT_ENERGY_COST + " 点）");

        // 1.4 获取帖子
        Post post = postService.getById(postId);
        ThrowUtils.throwIf(post == null, ErrorCode.NOT_FOUND_ERROR, "帖子不存在");
        
        // 获取帖子作者
        Agent postAuthor = agentService.getById(post.getAgentId());

        // 1.5 获取父评论（如果是回复）
        Comment parentComment = null;
        Agent parentCommentAuthor = null;
        Long rootCommentId = null;
        
        if (parentCommentId != null) {
            parentComment = this.getById(parentCommentId);
            ThrowUtils.throwIf(parentComment == null, 
                               ErrorCode.NOT_FOUND_ERROR, "父评论不存在");
            ThrowUtils.throwIf(!parentComment.getPostId().equals(postId), 
                               ErrorCode.PARAMS_ERROR, "父评论不属于该帖子");
            // 计算 rootCommentId
            rootCommentId = (parentComment.getRootCommentId() != null) 
                            ? parentComment.getRootCommentId() 
                            : parentComment.getId();
            
            // 获取父评论作者
            parentCommentAuthor = agentService.getById(parentComment.getAgentId());
        }

        // 1.6 获取物种信息
        Species species = speciesService.getById(agent.getSpeciesId());

        // ========== 2. AI 生成阶段 ==========
        
        AiCommentGenerateResult aiResult = aiCommentGeneratorService.generateComment(
                agent, species, post, postAuthor, parentComment, parentCommentAuthor);

        // ========== 3. 保存评论 ==========
        
        Comment comment = Comment.builder()
                .postId(postId)
                .agentId(agentId)
                .content(aiResult.getContent())
                .parentCommentId(parentCommentId)
                .rootCommentId(rootCommentId)
                .replyCount(0)
                .createTime(LocalDateTime.now())
                .updateTime(LocalDateTime.now())
                .isDelete(0)
                .build();

        boolean saved = this.save(comment);
        ThrowUtils.throwIf(!saved, ErrorCode.OPERATION_ERROR, "评论失败");

        // 3.1 如果是一级评论，rootCommentId 指向自己
        if (parentCommentId == null) {
            comment.setRootCommentId(comment.getId());
            this.updateById(comment);
        }

        // 3.2 更新父评论的 replyCount
        if (parentComment != null) {
            parentComment.setReplyCount(parentComment.getReplyCount() + 1);
            this.updateById(parentComment);
        }

        // ========== 4. 处理 Like/Dislike ==========
        
        handlePostLike(agentId, postId, aiResult);

        // ========== 5. 更新帖子评论数 ==========
        
        post.setCommentCount(post.getCommentCount() + 1);
        postService.updateById(post);

        // ========== 6. 更新 Agent 状态 ==========
        
        agent.setEnergy(agent.getEnergy() - COMMENT_ENERGY_COST);
        agent.setCommentCount(agent.getCommentCount() + 1);
        agentService.updateById(agent);

        log.info("Agent {} generated comment on post {}", agentId, postId);

        return CommentVO.objToVo(comment);
    }

    /**
     * 处理点赞/踩逻辑
     */
    private void handlePostLike(Long agentId, Long postId, AiCommentGenerateResult aiResult) {
        // 如果既不 like 也不 dislike，直接返回
        if ((aiResult.getLike() == null || !aiResult.getLike()) && 
            (aiResult.getDislike() == null || !aiResult.getDislike())) {
            return;
        }

        // 检查是否已存在 like/dislike 记录
        PostLike existingLike = postLikeService.lambdaQuery()
                .eq(PostLike::getAgentId, agentId)
                .eq(PostLike::getPostId, postId)
                .one();

        String newType = aiResult.getLike() != null && aiResult.getLike() ? "like" : "dislike";

        if (existingLike != null) {
            // 如果类型相同，不做任何操作
            if (existingLike.getType().equals(newType)) {
                return;
            }
            // 类型不同，更新记录
            existingLike.setType(newType);
            existingLike.setUpdateTime(LocalDateTime.now());
            postLikeService.updateById(existingLike);

            // 更新帖子计数（旧的 -1，新的 +1）
            Post post = postService.getById(postId);
            if ("like".equals(newType)) {
                post.setLikeCount(post.getLikeCount() + 1);
                post.setDislikeCount(Math.max(0, post.getDislikeCount() - 1));
            } else {
                post.setDislikeCount(post.getDislikeCount() + 1);
                post.setLikeCount(Math.max(0, post.getLikeCount() - 1));
            }
            postService.updateById(post);
        } else {
            // 新建记录
            PostLike postLike = PostLike.builder()
                    .postId(postId)
                    .agentId(agentId)
                    .type(newType)
                    .createTime(LocalDateTime.now())
                    .updateTime(LocalDateTime.now())
                    .isDelete(0)
                    .build();
            postLikeService.save(postLike);

            // 更新帖子计数
            Post post = postService.getById(postId);
            if ("like".equals(newType)) {
                post.setLikeCount(post.getLikeCount() + 1);
            } else {
                post.setDislikeCount(post.getDislikeCount() + 1);
            }
            postService.updateById(post);
        }
    }
}
