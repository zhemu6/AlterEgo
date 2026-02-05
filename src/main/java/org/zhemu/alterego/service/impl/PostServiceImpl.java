package org.zhemu.alterego.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
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
import org.zhemu.alterego.model.dto.post.PostQueryRequest;
import org.zhemu.alterego.model.entity.Agent;
import org.zhemu.alterego.model.entity.Post;
import org.zhemu.alterego.model.entity.PostLike;
import org.zhemu.alterego.model.entity.PostTag;
import org.zhemu.alterego.model.entity.Species;
import org.zhemu.alterego.model.entity.Tag;
import org.zhemu.alterego.model.vo.AgentVO;
import org.zhemu.alterego.model.vo.PostVO;
import org.zhemu.alterego.model.vo.SpeciesVO;
import org.zhemu.alterego.service.*;

import static org.zhemu.alterego.constant.Constants.POST_ENERGY_COST;

import java.time.LocalDateTime;
import java.util.ArrayList;
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
public class PostServiceImpl extends ServiceImpl<PostMapper, Post>
        implements PostService {

    private final AgentService agentService;
    private final SpeciesService speciesService;
    private final AiPostGeneratorService aiPostGeneratorService;
    private final PostLikeService postLikeService;
    private final TagService tagService;
    private final PostTagService postTagService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PostVO aiGeneratePost(AgentPostGenerateRequest request, Long userId) {
        Long agentId = request.getAgentId();

        // 1. 获取并校验Agent
        Agent agent = agentService.getById(agentId);
        ThrowUtils.throwIf(agent == null, ErrorCode.NOT_FOUND_ERROR, "Agent不存在?");

        // 2. 校验归属
        ThrowUtils.throwIf(!agent.getUserId().equals(userId), ErrorCode.NO_AUTH_ERROR, "只能操作自己的Agent");

        // 3. 校验能量
        ThrowUtils.throwIf(agent.getEnergy() < POST_ENERGY_COST, ErrorCode.OPERATION_ERROR, "能量不足，无法发帖（需" + POST_ENERGY_COST + " 点）");

        // 4. 获取物种信息
        Species species = speciesService.getById(agent.getSpeciesId());

        // 5. 调用 AI 生成内容
        AiPostGenerateResult aiResult = aiPostGeneratorService.generatePost(agent, species);

        // 6. 保存帖子
        Post post = Post.builder()
                .agentId(agentId)
                .title(aiResult.title)
                .content(aiResult.content)
                .postType("normal")
                // 转换�?JSON 字符串存�?
                .likeCount(0)
                .dislikeCount(0)
                .commentCount(0)
                .createTime(LocalDateTime.now())
                .updateTime(LocalDateTime.now())
                .isDelete(0)
                .build();

        boolean saved = this.save(post);
        tagService.savePostTags(post.getId(), aiResult.tags);
        ThrowUtils.throwIf(!saved, ErrorCode.OPERATION_ERROR, "发帖失败");


        boolean agentUpdated = agentService.lambdaUpdate()
                .eq(Agent::getId, agentId)
                .setSql("energy = energy - " + POST_ENERGY_COST + ", post_count = post_count + 1")
                .update();
        ThrowUtils.throwIf(!agentUpdated, ErrorCode.OPERATION_ERROR, "Agent能量更新失败");
        log.info("Agent {} post generated successfully: {}", agentId, post.getId());

        return PostVO.objToVo(post);
    }

    @Override
    public Page<PostVO> listPostByPage(PostQueryRequest postQueryRequest) {
        long current = postQueryRequest.getPageNum();
        long size = postQueryRequest.getPageSize();
        String sortField = postQueryRequest.getSortField();
        String sortOrder = postQueryRequest.getSortOrder();
        String searchText = postQueryRequest.getSearchText();
        String postType = postQueryRequest.getPostType();
        Long agentId = postQueryRequest.getAgentId();

        // 1. 构建查询条件
        LambdaQueryWrapper<Post> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(StrUtil.isNotBlank(postType), Post::getPostType, postType);
        queryWrapper.eq(agentId != null, Post::getAgentId, agentId);

        // 搜索：标题或内容包含关键�?
        if (StrUtil.isNotBlank(searchText)) {
            queryWrapper.and(qw -> qw.like(Post::getTitle, searchText).or().like(Post::getContent, searchText));
        }

        // 排序
        queryWrapper.orderBy(StrUtil.isNotBlank(sortField), "ascend".equals(sortOrder),
                "createTime".equals(sortField) ? Post::getCreateTime : Post::getId);
        // 默认按创建时间倒序
        if (StrUtil.isBlank(sortField)) {
            queryWrapper.orderByDesc(Post::getCreateTime);
        }

        // 2. 分页查询
        Page<Post> postPage = this.page(new Page<>(current, size), queryWrapper);
        List<Post> postList = postPage.getRecords();

        // 3. 数据转换与填充
        Page<PostVO> postVOPage = new Page<>(postPage.getCurrent(), postPage.getSize(), postPage.getTotal());
        if (CollUtil.isEmpty(postList)) {
            return postVOPage;
        }

        // 3.1 获取所有相关的 AgentId
        Set<Long> agentIds = postList.stream().map(Post::getAgentId).collect(Collectors.toSet());

        // 3.2 批量查询 Agent
        Map<Long, Agent> agentMap = agentService.listByIds(agentIds).stream()
                .collect(Collectors.toMap(Agent::getId, agent -> agent));

        // 3.3 获取所有相关的 SpeciesId
        Set<Long> speciesIds = agentMap.values().stream().map(Agent::getSpeciesId).collect(Collectors.toSet());

        // 3.4 批量查询 Species
        Map<Long, Species> speciesMap = speciesService.listByIds(speciesIds).stream()
                .collect(Collectors.toMap(Species::getId, species -> species));

        // 3.5 组装 PostVO
        List<PostVO> postVOList = postList.stream().map(post -> {
            PostVO postVO = PostVO.objToVo(post);
            Long userId = agentMap.get(post.getAgentId()).getUserId();
            Agent agent = agentMap.get(post.getAgentId());
            if (agent != null) {
                Species species = speciesMap.get(agent.getSpeciesId());
                SpeciesVO speciesVO = SpeciesVO.objToVo(species);
                AgentVO agentVO = AgentVO.objToVo(agent, speciesVO);
                postVO.setAgent(agentVO);
            }
            return postVO;
        }).collect(Collectors.toList());

        fillPostTags(postVOList);
        postVOPage.setRecords(postVOList);
        return postVOPage;
    }

    @Override
    public PostVO getPostVOById(Long id, Long userId) {
        Post post = this.getById(id);
        ThrowUtils.throwIf(post == null, ErrorCode.NOT_FOUND_ERROR);

        PostVO postVO = PostVO.objToVo(post);
        fillPostTags(List.of(postVO));

        // 填充 Agent 的 Species
        Agent agent = agentService.getById(post.getAgentId());
        if (agent != null) {
            Species species = speciesService.getById(agent.getSpeciesId());
            postVO.setAgent(AgentVO.objToVo(agent, SpeciesVO.objToVo(species)));
        }

        // 填充点赞状�?
        if (userId != null) {
            // 查询当前用户�?Agent
            Agent userAgent = agentService.lambdaQuery()
                    .eq(Agent::getUserId, userId)
                    .one();

            if (userAgent != null) {
                PostLike postLike = postLikeService.lambdaQuery()
                        .eq(PostLike::getPostId, id)
                        .eq(PostLike::getAgentId, userAgent.getId())
                        .one();
                if (postLike != null) {
                    postVO.setHasLiked(postLike.getLikeType());
                } else {
                    postVO.setHasLiked(0);
                }
            }
        } else {
            postVO.setHasLiked(0);
        }

        return postVO;
    }

    private void fillPostTags(List<PostVO> postVOList) {
        if (CollUtil.isEmpty(postVOList)) {
            return;
        }
        Set<Long> postIds = postVOList.stream()
                .map(PostVO::getId)
                .filter(id -> id != null)
                .collect(Collectors.toSet());
        if (CollUtil.isEmpty(postIds)) {
            return;
        }
        List<PostTag> postTags = postTagService.lambdaQuery()
                .in(PostTag::getPostId, postIds)
                .orderByAsc(PostTag::getCreateTime)
                .list();
        if (CollUtil.isEmpty(postTags)) {
            return;
        }
        Set<Long> tagIds = postTags.stream()
                .map(PostTag::getTagId)
                .filter(id -> id != null)
                .collect(Collectors.toSet());
        if (CollUtil.isEmpty(tagIds)) {
            return;
        }
        Map<Long, Tag> tagMap = tagService.listByIds(tagIds).stream()
                .collect(Collectors.toMap(Tag::getId, tag -> tag));
        Map<Long, List<String>> postIdToTags = new HashMap<>();
        for (PostTag postTag : postTags) {
            Tag tag = tagMap.get(postTag.getTagId());
            if (tag == null) {
                continue;
            }
            postIdToTags.computeIfAbsent(postTag.getPostId(), key -> new ArrayList<>())
                    .add(tag.getName());
        }
        for (PostVO postVO : postVOList) {
            if (postVO == null || postVO.getId() == null) {
                continue;
            }
            List<String> tags = postIdToTags.get(postVO.getId());
            if (tags != null) {
                postVO.setTags(tags);
            }
        }
    }
}
