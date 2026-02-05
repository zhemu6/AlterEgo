package org.zhemu.alterego.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.zhemu.alterego.exception.BusinessException;
import org.zhemu.alterego.exception.ErrorCode;
import org.zhemu.alterego.exception.ThrowUtils;
import org.zhemu.alterego.model.dto.pk.AiPkGenerateResult;
import org.zhemu.alterego.model.dto.pk.AiPkVoteResult;
import org.zhemu.alterego.model.dto.pk.PkCreateRequest;
import org.zhemu.alterego.model.dto.pk.PkQueryRequest;
import org.zhemu.alterego.model.dto.pk.PkVoteRequest;
import org.zhemu.alterego.model.entity.Agent;
import org.zhemu.alterego.model.entity.AgentVoteRecord;
import org.zhemu.alterego.model.entity.Comment;
import org.zhemu.alterego.model.entity.PkVoteOption;
import org.zhemu.alterego.model.entity.Post;
import org.zhemu.alterego.model.entity.PostTag;
import org.zhemu.alterego.model.entity.Species;
import org.zhemu.alterego.model.entity.Tag;
import org.zhemu.alterego.model.vo.PkPostVO;
import org.zhemu.alterego.model.vo.PkVoteOptionVO;
import org.zhemu.alterego.model.vo.PostVO;
import org.zhemu.alterego.service.AgentService;
import org.zhemu.alterego.service.AgentVoteRecordService;
import org.zhemu.alterego.service.AiPkGeneratorService;
import org.zhemu.alterego.service.AiPkVoteGeneratorService;
import org.zhemu.alterego.service.CommentService;
import org.zhemu.alterego.service.PkService;
import org.zhemu.alterego.service.PkVoteOptionService;
import org.zhemu.alterego.service.PostService;
import org.zhemu.alterego.service.PostTagService;
import org.zhemu.alterego.service.SpeciesService;
import org.zhemu.alterego.service.TagService;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static org.zhemu.alterego.constant.Constants.PK_CREATE_ENERGY_COST;
import static org.zhemu.alterego.constant.Constants.PK_DURATION_HOURS;
import static org.zhemu.alterego.constant.Constants.PK_VOTE_ENERGY_COST;

/**
 * PK 服务实现类
 * @author lushihao
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PkServiceImpl implements PkService {

    private final AgentService agentService;
    private final SpeciesService speciesService;
    private final PostService postService;
    private final PkVoteOptionService pkVoteOptionService;
    private final AgentVoteRecordService agentVoteRecordService;
    private final AiPkGeneratorService aiPkGeneratorService;
    private final AiPkVoteGeneratorService aiPkVoteGeneratorService;
    private final CommentService commentService;
    private final TagService tagService;
    private final PostTagService postTagService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PkPostVO createPk(PkCreateRequest request, Long userId) {
        Long agentId = request.getAgentId();
        
        // 1. 获取并校验 Agent
        Agent agent = agentService.getById(agentId);
        ThrowUtils.throwIf(agent == null, ErrorCode.NOT_FOUND_ERROR, "Agent不存在");
        
        // 2. 校验归属
        ThrowUtils.throwIf(!agent.getUserId().equals(userId), ErrorCode.NO_AUTH_ERROR, "只能操作自己的Agent");
        
        // 3. 原子能量检查+扣除（CRITICAL: 修复并发问题）
        boolean energyOk = agentService.lambdaUpdate()
            .eq(Agent::getId, agentId)
            .ge(Agent::getEnergy, PK_CREATE_ENERGY_COST)  // 原子检查
            .setSql("energy = energy - " + PK_CREATE_ENERGY_COST + ", post_count = post_count + 1")
            .update();
        ThrowUtils.throwIf(!energyOk, ErrorCode.OPERATION_ERROR, "能量不足或并发冲突（需" + PK_CREATE_ENERGY_COST + " 点）");
        
        // 4. 获取物种信息
        Species species = speciesService.getById(agent.getSpeciesId());
        
        // 5. 调用 AI 生成 PK 话题
        AiPkGenerateResult aiResult = aiPkGeneratorService.generatePk(agent, species);
        
        // 6. 保存 Post（postType = 'pk'）
        Post post = Post.builder()
            .agentId(agentId)
            .postType("pk")
            .title(aiResult.topic)
            .content(aiResult.description)
            .likeCount(0)
            .dislikeCount(0)
            .commentCount(0)
            .createTime(LocalDateTime.now())
            .updateTime(LocalDateTime.now())
            .isDelete(0)
            .build();
        boolean saved = postService.save(post);
        ThrowUtils.throwIf(!saved, ErrorCode.OPERATION_ERROR, "发起PK失败");
        
        // 7. 保存两个 PkVoteOption（status='active', endTime=now+24h）
        LocalDateTime endTime = LocalDateTime.now().plusHours(PK_DURATION_HOURS);
        PkVoteOption optionA = PkVoteOption.builder()
            .postId(post.getId())
            .optionText(aiResult.optionA)
            .voteCount(0)
            .status("active")
            .endTime(endTime)
            .createTime(LocalDateTime.now())
            .updateTime(LocalDateTime.now())
            .isDelete(0)
            .build();
        PkVoteOption optionB = PkVoteOption.builder()
            .postId(post.getId())
            .optionText(aiResult.optionB)
            .voteCount(0)
            .status("active")
            .endTime(endTime)
            .createTime(LocalDateTime.now())
            .updateTime(LocalDateTime.now())
            .isDelete(0)
            .build();
        pkVoteOptionService.saveBatch(Arrays.asList(optionA, optionB));
        
        // 8. 保存标签
        savePostTags(post.getId(), aiResult.tags);
        
        log.info("Agent {} created PK successfully: {}", agentId, post.getId());
        
        // 9. 返回 PkPostVO
        return getPkById(post.getId(), userId);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public PkPostVO vote(PkVoteRequest request, Long userId) {
        Long agentId = request.getAgentId();
        Long postId = request.getPostId();
        
        // 1. 校验 Agent 存在性和归属
        Agent agent = agentService.getById(agentId);
        ThrowUtils.throwIf(agent == null, ErrorCode.NOT_FOUND_ERROR, "Agent不存在");
        ThrowUtils.throwIf(!agent.getUserId().equals(userId), ErrorCode.NO_AUTH_ERROR, "只能操作自己的Agent");
        
        // 2. 校验 Post 存在且为 PK 类型
        Post post = postService.getById(postId);
        ThrowUtils.throwIf(post == null, ErrorCode.NOT_FOUND_ERROR, "PK帖子不存在");
        ThrowUtils.throwIf(!"pk".equals(post.getPostType()), ErrorCode.PARAMS_ERROR, "该帖子不是PK类型");
        
        // 3. 获取 PkVoteOption 列表
        List<PkVoteOption> options = pkVoteOptionService.lambdaQuery()
            .eq(PkVoteOption::getPostId, postId)
            .orderByAsc(PkVoteOption::getId)
            .list();
        ThrowUtils.throwIf(options.size() != 2, ErrorCode.SYSTEM_ERROR, "PK选项数据异常");
        
        PkVoteOption optionA = options.get(0);
        PkVoteOption optionB = options.get(1);
        
        // 4. 校验 status='active'
        ThrowUtils.throwIf(!"active".equals(optionA.getStatus()), ErrorCode.OPERATION_ERROR, "PK已结束");
        
        // 5. 校验未过期
        ThrowUtils.throwIf(optionA.getEndTime().isBefore(LocalDateTime.now()), ErrorCode.OPERATION_ERROR, "PK投票已过期");
        
        // 6. 原子能量检查+扣除
        boolean energyOk = agentService.lambdaUpdate()
            .eq(Agent::getId, agentId)
            .ge(Agent::getEnergy, PK_VOTE_ENERGY_COST)  // 原子检查
            .setSql("energy = energy - " + PK_VOTE_ENERGY_COST + ", comment_count = comment_count + 1")
            .update();
        ThrowUtils.throwIf(!energyOk, ErrorCode.OPERATION_ERROR, "能量不足或并发冲突（需" + PK_VOTE_ENERGY_COST + " 点）");
        
        // 7. 获取物种信息
        Species species = speciesService.getById(agent.getSpeciesId());
        
        // 8. 调用 AI 生成投票选择
        AiPkVoteResult aiResult = aiPkVoteGeneratorService.generateVote(agent, species, post, optionA, optionB);
        
        // 9. 解析 selectedOption（A/B → optionId）
        Long selectedOptionId;
        if ("A".equalsIgnoreCase(aiResult.selectedOption)) {
            selectedOptionId = optionA.getId();
        } else if ("B".equalsIgnoreCase(aiResult.selectedOption)) {
            selectedOptionId = optionB.getId();
        } else {
            log.warn("AI returned invalid selectedOption: {}, defaulting to A", aiResult.selectedOption);
            selectedOptionId = optionA.getId();
        }
        
        // 10. 保存投票记录（捕获 DuplicateKeyException）
        try {
            AgentVoteRecord voteRecord = AgentVoteRecord.builder()
                .agentId(agentId)
                .postId(postId)
                .optionId(selectedOptionId)
                .createTime(LocalDateTime.now())
                .updateTime(LocalDateTime.now())
                .isDelete(0)
                .build();
            agentVoteRecordService.save(voteRecord);
        } catch (DuplicateKeyException e) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "你的 Agent 已经投过票了");
        }
        
        // 11. 原子更新选项票数
        pkVoteOptionService.lambdaUpdate()
            .eq(PkVoteOption::getId, selectedOptionId)
            .setSql("vote_count = vote_count + 1")
            .update();
        
        // 12. 保存评论（AI 生成的 reason 作为评论内容）
        Comment comment = Comment.builder()
            .postId(postId)
            .agentId(agentId)
            .content(aiResult.reason)
            .parentCommentId(null)  // 顶级评论
            .rootCommentId(null)    // 将在保存后设置为自己的ID（或由数据库触发器处理）
            .replyCount(0)
            .likeCount(0)
            .dislikeCount(0)
            .createTime(LocalDateTime.now())
            .updateTime(LocalDateTime.now())
            .isDelete(0)
            .build();
        commentService.save(comment);
        
        // 更新 post.comment_count
        postService.lambdaUpdate()
            .eq(Post::getId, postId)
            .setSql("comment_count = comment_count + 1")
            .update();
        
        log.info("Agent {} voted on PK {}, option: {}", agentId, postId, aiResult.selectedOption);
        
        // 13. 返回 PkPostVO
        return getPkById(postId, userId);
    }
    
    @Override
    public Page<PkPostVO> listPkByPage(PkQueryRequest request, Long userId) {
        // 1. 构建查询条件
        LambdaQueryWrapper<Post> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Post::getPostType, "pk");
        
        // 可选过滤：agentId
        if (request.getAgentId() != null) {
            wrapper.eq(Post::getAgentId, request.getAgentId());
        }
        
        // 排序
        String sortField = request.getSortField();
        String sortOrder = request.getSortOrder();
        if (sortField != null && !sortField.isEmpty()) {
            boolean isAsc = "ascend".equals(sortOrder);
            if ("createTime".equals(sortField)) {
                wrapper.orderBy(true, isAsc, Post::getCreateTime);
            }
        } else {
            wrapper.orderByDesc(Post::getCreateTime);  // 默认按创建时间倒序
        }
        
        // 2. 分页查询 Post
        Page<Post> postPage = postService.page(
            new Page<>(request.getPageNum(), request.getPageSize()),
            wrapper
        );
        
        // 3. 批量转换为 PkPostVO
        List<PkPostVO> pkPostVOList = postPage.getRecords().stream()
            .map(post -> {
                try {
                    return getPkById(post.getId(), userId);
                } catch (Exception e) {
                    log.error("Error converting post {} to PkPostVO", post.getId(), e);
                    return null;
                }
            })
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
        
        // 4. 返回分页结果
        Page<PkPostVO> resultPage = new Page<>(request.getPageNum(), request.getPageSize(), postPage.getTotal());
        resultPage.setRecords(pkPostVOList);
        return resultPage;
    }
    
    @Override
    public PkPostVO getPkById(Long postId, Long userId) {
        // 1. 查询 Post
        Post post = postService.getById(postId);
        ThrowUtils.throwIf(post == null, ErrorCode.NOT_FOUND_ERROR, "帖子不存在");
        ThrowUtils.throwIf(!"pk".equals(post.getPostType()), ErrorCode.PARAMS_ERROR, "该帖子不是PK类型");
        
        // 2. 查询两个 PkVoteOption
        List<PkVoteOption> options = pkVoteOptionService.lambdaQuery()
            .eq(PkVoteOption::getPostId, postId)
            .orderByAsc(PkVoteOption::getId)
            .list();
        ThrowUtils.throwIf(options.isEmpty(), ErrorCode.SYSTEM_ERROR, "PK选项不存在");
        
        // 3. 计算总票数
        int totalVotes = options.stream()
            .mapToInt(PkVoteOption::getVoteCount)
            .sum();
        
        // 4. 转换为 PkVoteOptionVO
        List<PkVoteOptionVO> optionVOs = options.stream()
            .map(option -> PkVoteOptionVO.objToVo(option, totalVotes))
            .collect(Collectors.toList());
        
        // 5. 查询当前用户 Agent 的投票记录
        Agent userAgent = agentService.lambdaQuery()
            .eq(Agent::getUserId, userId)
            .one();
        
        Boolean hasVoted = false;
        Long votedOptionId = null;
        if (userAgent != null) {
            AgentVoteRecord voteRecord = agentVoteRecordService.lambdaQuery()
                .eq(AgentVoteRecord::getAgentId, userAgent.getId())
                .eq(AgentVoteRecord::getPostId, postId)
                .one();
            if (voteRecord != null) {
                hasVoted = true;
                votedOptionId = voteRecord.getOptionId();
            }
        }
        
        // 6. 转换 Post 为 PostVO
        PostVO postVO = PostVO.objToVo(post);
        
        // 7. 组装 PkPostVO
        PkPostVO pkPostVO = PkPostVO.fromPostVO(postVO);
        pkPostVO.setOptions(optionVOs);
        pkPostVO.setStatus(options.get(0).getStatus());
        pkPostVO.setEndTime(options.get(0).getEndTime());
        pkPostVO.setTotalVotes(totalVotes);
        pkPostVO.setHasVoted(hasVoted);
        pkPostVO.setVotedOptionId(votedOptionId);
        
        return pkPostVO;
    }
    
    @Override
    public int closeExpiredPks() {
        int updated = pkVoteOptionService.lambdaUpdate()
            .eq(PkVoteOption::getStatus, "active")
            .lt(PkVoteOption::getEndTime, LocalDateTime.now())
            .set(PkVoteOption::getStatus, "closed")
            .update() ? 1 : 0;
        
        if (updated > 0) {
            log.info("Closed {} expired PKs", updated);
        }
        return updated;
    }
    
    /**
     * 标签保存辅助方法（参考 PostServiceImpl）
     */
    private void savePostTags(Long postId, List<String> rawTags) {
        if (rawTags == null || rawTags.isEmpty()) {
            return;
        }
        
        for (String raw : rawTags) {
            // 使用 TagService 的 getOrCreateTag 方法
            Tag tag = tagService.getOrCreateTag(raw);
            if (tag == null || tag.getId() == null) {
                continue;
            }
            
            // 创建帖子-标签关联
            PostTag postTag = PostTag.builder()
                .postId(postId)
                .tagId(tag.getId())
                .createTime(LocalDateTime.now())
                .build();
            
            try {
                boolean relationSaved = postTagService.save(postTag);
                if (relationSaved) {
                    tagService.lambdaUpdate()
                        .eq(Tag::getId, tag.getId())
                        .setSql("post_count = post_count + 1")
                        .update();
                }
            } catch (DuplicateKeyException e) {
                // 忽略重复关联
                log.debug("Duplicate post-tag relation ignored: postId={}, tagId={}", postId, tag.getId());
            }
        }
    }
}
