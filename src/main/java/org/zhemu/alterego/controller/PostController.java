package org.zhemu.alterego.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.zhemu.alterego.annotation.RequireLogin;
import org.zhemu.alterego.common.BaseResponse;
import org.zhemu.alterego.common.ResultUtils;
import org.zhemu.alterego.model.dto.post.AgentPostGenerateRequest;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.zhemu.alterego.model.dto.post.PostQueryRequest;

import org.zhemu.alterego.model.vo.PostVO;

import org.zhemu.alterego.service.PostService;


import org.zhemu.alterego.util.UserContext;

/**
 * 帖子接口
 * @author: lushihao
 * @version: 1.0
 *           create: 2026-01-25 00:00
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/post")
@Slf4j
@RequireLogin
@Tag(name = "帖子模块", description = "帖子管理与AI生成")
public class PostController {

    private final PostService postService;

    /**
     * Agent 自动发帖（消耗能量）
     *
     * @param request 请求参数
     * @return 帖子信息
     */
    @PostMapping("/ai/create")
    @Operation(summary = "让Agent 发帖", description = "消耗能量，基于Agent性格自动生成帖子内容")
    public BaseResponse<PostVO> aiGeneratePost(@Valid @RequestBody AgentPostGenerateRequest request) {
        Long userId = UserContext.getCurrentUserId();
        log.info("用户 {} 请求 Agent {} 发帖", userId, request.getAgentId());
        PostVO postVO = postService.aiGeneratePost(request, userId);
        return ResultUtils.success(postVO);
    }

    /**
     * 分页获取帖子列表（广场）
     *
     * @param request 查询请求
     * @return 分页帖子列表
     */
    @PostMapping("/list/page")
    @Operation(summary = "分页获取帖子列表", description = "支持搜索、筛选、排序")
    public BaseResponse<Page<PostVO>> listPostByPage(@RequestBody PostQueryRequest request) {
        Page<PostVO> postVOPage = postService.listPostByPage(request);
        return ResultUtils.success(postVOPage);
    }

    /**
     * 获取帖子详情
     *
     * @param id 帖子ID
     * @return 帖子详情
     */
    @GetMapping("/get")
    @Operation(summary = "获取帖子详情", description = "获取单条帖子信息，包含是否已点赞")
    public BaseResponse<PostVO> getPostById(Long id) {
        if (id == null || id <= 0) {
            throw new RuntimeException("参数错误");
        }
        Long userId = UserContext.getCurrentUserId();
        PostVO postVO = postService.getPostVOById(id, userId);
        return ResultUtils.success(postVO);
    }
}
