package org.zhemu.alterego.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
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
import org.zhemu.alterego.model.dto.comment.CommentQueryRequest;
import org.zhemu.alterego.model.entity.*;
import org.zhemu.alterego.model.enums.LikeTypeEnum;
import org.zhemu.alterego.model.vo.AgentVO;
import org.zhemu.alterego.model.vo.CommentPageVO;
import org.zhemu.alterego.model.vo.CommentVO;
import org.zhemu.alterego.model.vo.SpeciesVO;
import org.zhemu.alterego.service.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

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
    private final CommentLikeService commentLikeService;
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
            
            // 禁止回复自己的评论
            ThrowUtils.throwIf(parentComment.getAgentId().equals(agentId),
                               ErrorCode.OPERATION_ERROR, "不能回复自己的评论");

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
            this.lambdaUpdate()
                    .eq(Comment::getId, parentComment.getId())
                    .setSql("reply_count = reply_count + 1")
                    .update();

            // 如果父评论不是根评论，也要更新根评论的回复数（用于两级评论展示）
            if (!parentComment.getId().equals(rootCommentId)) {
                this.lambdaUpdate()
                        .eq(Comment::getId, rootCommentId)
                        .setSql("reply_count = reply_count + 1")
                        .update();
            }
        }

        // ========== 4. 处理 Like/Dislike ==========
        
        if (parentCommentId != null) {
            handleCommentLike(agentId, parentCommentId, aiResult);
        } else {
            handlePostLike(agentId, postId, aiResult);
        }

        // ========== 5. 更新帖子评论数 ==========
        
        postService.lambdaUpdate()
                .eq(Post::getId, postId)
                .setSql("comment_count = comment_count + 1")
                .update();

        // ========== 6. 更新 Agent 状态 ==========
        
        agentService.lambdaUpdate()
                .eq(Agent::getId, agentId)
                .setSql("energy = energy - " + COMMENT_ENERGY_COST + ", comment_count = comment_count + 1")
                .update();

        log.info("Agent {} generated comment on post {}", agentId, postId);

        return CommentVO.objToVo(comment);
    }

    @Override
    public CommentPageVO listCommentByPage(CommentQueryRequest request, Long userId) {
        long current = 1;
        long size = request.getPageSize();
        Long postId = request.getPostId();
        Long rootCommentId = request.getRootCommentId();
        LocalDateTime cursorTime = request.getCursorTime();
        Long cursorId = request.getCursorId();

        LambdaQueryWrapper<Comment> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Comment::getPostId, postId);

        if (rootCommentId == null) {
            // 查询一级评论（仅游标模式）
            queryWrapper.isNull(Comment::getParentCommentId);
            if (cursorTime != null && cursorId != null) {
                queryWrapper.and(qw -> qw.lt(Comment::getCreateTime, cursorTime)
                        .or()
                        .eq(Comment::getCreateTime, cursorTime)
                        .lt(Comment::getId, cursorId));
            }
        } else {
            // 查询子评论（仅游标模式，按时间顺序）
            queryWrapper.eq(Comment::getRootCommentId, rootCommentId);
            queryWrapper.isNotNull(Comment::getParentCommentId);
            // 子评论固定限制每页数量，避免一次拉取过多
            if (size > 20) {
                size = 20;
            }
            if (cursorTime != null && cursorId != null) {
                queryWrapper.and(qw -> qw.gt(Comment::getCreateTime, cursorTime)
                        .or()
                        .eq(Comment::getCreateTime, cursorTime)
                        .gt(Comment::getId, cursorId));
            }
        }

        // 排序（游标分页要求稳定顺序）
        if (rootCommentId == null) {
            queryWrapper.orderByDesc(Comment::getCreateTime, Comment::getId);
        } else {
            queryWrapper.orderByAsc(Comment::getCreateTime, Comment::getId);
        }

        long pageSize = size;
        Page<Comment> commentPage = this.page(new Page<>(current, pageSize + 1), queryWrapper);
        List<Comment> commentList = commentPage.getRecords();

        boolean hasMore = commentList.size() > pageSize;
        if (hasMore) {
            commentList = commentList.subList(0, (int) pageSize);
        }

        CommentPageVO commentVOPage = new CommentPageVO();
        commentVOPage.setPageSize(pageSize);
        commentVOPage.setTotal(commentPage.getTotal());
        commentVOPage.setHasMore(hasMore);

        if (CollUtil.isEmpty(commentList)) {
            commentVOPage.setRecords(List.of());
            return commentVOPage;
        }

        // 批量查询相关信息
        Set<Long> agentIds = commentList.stream().map(Comment::getAgentId).collect(Collectors.toSet());

        // 1. 获取所有父评论ID，用于查找被回复的Agent
        Set<Long> parentCommentIds = commentList.stream()
                .map(Comment::getParentCommentId)
                .filter(id -> id != null)
                .collect(Collectors.toSet());
        
        Map<Long, Long> parentCommentAgentIdMap = new HashMap<>();
        if (CollUtil.isNotEmpty(parentCommentIds)) {
            // 查出父评论，提取其 AgentId
            List<Comment> parentComments = this.listByIds(parentCommentIds);
            Map<Long, Long> map = parentComments.stream()
                    .collect(Collectors.toMap(Comment::getId, Comment::getAgentId));
            parentCommentAgentIdMap.putAll(map);
            // 将被回复的 AgentId 也加入待查询集合
            agentIds.addAll(map.values());
        }

        Map<Long, Agent> agentMap = agentService.listByIds(agentIds).stream()
                .collect(Collectors.toMap(Agent::getId, agent -> agent));

        // 物种
        Set<Long> speciesIds = agentMap.values().stream().map(Agent::getSpeciesId).collect(Collectors.toSet());
        Map<Long, Species> speciesMap = speciesService.listByIds(speciesIds).stream()
                .collect(Collectors.toMap(Species::getId, species -> species));

        // 点赞状态
        Map<Long, Integer> likeStatusMap = new HashMap<>();
        if (userId != null) {
            Agent userAgent = agentService.lambdaQuery().eq(Agent::getUserId, userId).one();
            if (userAgent != null) {
                Set<Long> commentIds = commentList.stream().map(Comment::getId).collect(Collectors.toSet());
                if (CollUtil.isNotEmpty(commentIds)) {
                    List<CommentLike> likes = commentLikeService.lambdaQuery()
                            .in(CommentLike::getCommentId, commentIds)
                            .eq(CommentLike::getAgentId, userAgent.getId())
                            .list();
                    likeStatusMap = likes.stream()
                            .collect(Collectors.toMap(CommentLike::getCommentId, CommentLike::getLikeType));
                }
            }
        }
        Map<Long, Integer> finalLikeStatusMap = likeStatusMap;

        List<CommentVO> commentVOList = commentList.stream().map(comment -> {
            CommentVO commentVO = CommentVO.objToVo(comment);
            
            // 设置当前评论作者
            Agent agent = agentMap.get(comment.getAgentId());
            if (agent != null) {
                Species species = speciesMap.get(agent.getSpeciesId());
                commentVO.setAgent(AgentVO.objToVo(agent, SpeciesVO.objToVo(species)));
            }
            
            // 设置被回复的 Agent (如果存在父评论)
            if (comment.getParentCommentId() != null) {
                Long targetAgentId = parentCommentAgentIdMap.get(comment.getParentCommentId());
                if (targetAgentId != null) {
                    Agent targetAgent = agentMap.get(targetAgentId);
                    if (targetAgent != null) {
                        Species targetSpecies = speciesMap.get(targetAgent.getSpeciesId());
                        commentVO.setReplyToAgent(AgentVO.objToVo(targetAgent, SpeciesVO.objToVo(targetSpecies)));
                    }
                }
            }
            
            commentVO.setHasLiked(finalLikeStatusMap.getOrDefault(comment.getId(), 0));
            return commentVO;
        }).collect(Collectors.toList());

        commentVOPage.setRecords(commentVOList);
        if (hasMore) {
            Comment last = commentList.get(commentList.size() - 1);
            commentVOPage.setNextCursorTime(last.getCreateTime());
            commentVOPage.setNextCursorId(last.getId());
        } else {
            commentVOPage.setNextCursorTime(null);
            commentVOPage.setNextCursorId(null);
        }
        return commentVOPage;
    }

    /**
     * 处理帖子点赞/踩逻辑
     */
    private void handlePostLike(Long agentId, Long postId, AiCommentGenerateResult aiResult) {
        log.info(aiResult.toString());
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

        // 获取新的类型
        LikeTypeEnum newTypeEnum = (aiResult.getLike() != null && aiResult.getLike()) 
                                   ? LikeTypeEnum.LIKE 
                                   : LikeTypeEnum.DISLIKE;
        Integer newTypeValue = newTypeEnum.getValue();

        if (existingLike != null) {
            // 如果类型相同，不做任何操作
            if (existingLike.getLikeType().equals(newTypeValue)) {
                return;
            }
            
            // 类型不同，更新记录
            // 如果原来是 LIKE (1)，现在变成 DISLIKE (2)：like_count - 1, dislike_count + 1
            // 如果原来是 DISLIKE (2)，现在变成 LIKE (1)：dislike_count - 1, like_count + 1
            boolean wasLike = LikeTypeEnum.LIKE.getValue() == existingLike.getLikeType();
            
            existingLike.setLikeType(newTypeValue);
            existingLike.setUpdateTime(LocalDateTime.now());
            postLikeService.updateById(existingLike);

            // 使用 setSql 原子更新
            if (wasLike) {
                // 原来是赞，现在变成踩
                postService.lambdaUpdate()
                        .eq(Post::getId, postId)
                        .setSql("like_count = GREATEST(like_count - 1, 0), dislike_count = dislike_count + 1")
                        .update();
            } else {
                // 原来是踩，现在变成赞
                postService.lambdaUpdate()
                        .eq(Post::getId, postId)
                        .setSql("dislike_count = GREATEST(dislike_count - 1, 0), like_count = like_count + 1")
                        .update();
            }
        } else {
            // 新建记录
            PostLike postLike = PostLike.builder()
                    .postId(postId)
                    .agentId(agentId)
                    .likeType(newTypeValue)
                    .createTime(LocalDateTime.now())
                    .updateTime(LocalDateTime.now())
                    .isDelete(0)
                    .build();
            postLikeService.save(postLike);

            // 原子更新计数
            if (newTypeEnum == LikeTypeEnum.LIKE) {
                postService.lambdaUpdate()
                        .eq(Post::getId, postId)
                        .setSql("like_count = like_count + 1")
                        .update();
            } else {
                postService.lambdaUpdate()
                        .eq(Post::getId, postId)
                        .setSql("dislike_count = dislike_count + 1")
                        .update();
            }
        }
    }

    /**
     * 处理评论点赞/踩逻辑
     */
    private void handleCommentLike(Long agentId, Long commentId, AiCommentGenerateResult aiResult) {
        // 如果既不 like 也不 dislike，直接返回
        if ((aiResult.getLike() == null || !aiResult.getLike()) && 
            (aiResult.getDislike() == null || !aiResult.getDislike())) {
            return;
        }

        // 检查是否已存在 like/dislike 记录
        CommentLike existingLike = commentLikeService.lambdaQuery()
                .eq(CommentLike::getAgentId, agentId)
                .eq(CommentLike::getCommentId, commentId)
                .one();

        // 获取新的类型
        LikeTypeEnum newTypeEnum = (aiResult.getLike() != null && aiResult.getLike()) 
                                   ? LikeTypeEnum.LIKE 
                                   : LikeTypeEnum.DISLIKE;
        Integer newTypeValue = newTypeEnum.getValue();

        if (existingLike != null) {
            // 如果类型相同，不做任何操作
            if (existingLike.getLikeType().equals(newTypeValue)) {
                return;
            }
            
            // 类型不同，更新记录
            boolean wasLike = LikeTypeEnum.LIKE.getValue() == existingLike.getLikeType();
            
            existingLike.setLikeType(newTypeValue);
            existingLike.setUpdateTime(LocalDateTime.now());
            commentLikeService.updateById(existingLike);

            // 原子更新计数
            if (wasLike) {
                // 原来是赞，现在变成踩
                this.lambdaUpdate()
                        .eq(Comment::getId, commentId)
                        .setSql("like_count = GREATEST(like_count - 1, 0), dislike_count = dislike_count + 1")
                        .update();
            } else {
                // 原来是踩，现在变成赞
                this.lambdaUpdate()
                        .eq(Comment::getId, commentId)
                        .setSql("dislike_count = GREATEST(dislike_count - 1, 0), like_count = like_count + 1")
                        .update();
            }
        } else {
            // 新建记录
            CommentLike commentLike = CommentLike.builder()
                    .commentId(commentId)
                    .agentId(agentId)
                    .likeType(newTypeValue)
                    .createTime(LocalDateTime.now())
                    .updateTime(LocalDateTime.now())
                    .isDelete(0)
                    .build();
            commentLikeService.save(commentLike);

            // 原子更新计数
            if (newTypeEnum == LikeTypeEnum.LIKE) {
                this.lambdaUpdate()
                        .eq(Comment::getId, commentId)
                        .setSql("like_count = like_count + 1")
                        .update();
            } else {
                this.lambdaUpdate()
                        .eq(Comment::getId, commentId)
                        .setSql("dislike_count = dislike_count + 1")
                        .update();
            }
        }
    }
}


