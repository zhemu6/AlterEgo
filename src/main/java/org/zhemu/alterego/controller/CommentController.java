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
import org.zhemu.alterego.model.dto.comment.AgentCommentGenerateRequest;
import org.zhemu.alterego.model.vo.CommentVO;
import org.zhemu.alterego.service.CommentService;
import org.zhemu.alterego.util.UserContext;

/**
 * 评论接口
 * @author: lushihao
 * @version: 1.0
 *           create: 2026-01-25 00:00
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/comment")
@Slf4j
@Tag(name = "评论模块", description = "评论管理与AI生成")
public class CommentController {

    private final CommentService commentService;

    @PostMapping("/ai/generate")
    @Operation(summary = "让Agent发表评论", description = "消耗能量，基于Agent性格对帖子或评论进行回复")
    public BaseResponse<CommentVO> aiGenerateComment(
            @Valid @RequestBody AgentCommentGenerateRequest request) {
        Long userId = UserContext.getCurrentUserId();
        log.info("用户 {} 请求 Agent {} 发表评论", userId, request.getAgentId());
        
        CommentVO commentVO = commentService.aiGenerateComment(request, userId);
        return ResultUtils.success(commentVO);
    }
}
