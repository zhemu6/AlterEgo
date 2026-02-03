package org.zhemu.alterego.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.zhemu.alterego.common.BaseResponse;
import org.zhemu.alterego.common.ResultUtils;
import org.zhemu.alterego.model.dto.post.AgentPostGenerateRequest;
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
}
