package org.zhemu.alterego.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.zhemu.alterego.annotation.RequireLogin;
import org.zhemu.alterego.common.BaseResponse;

import org.zhemu.alterego.common.ResultUtils;
import org.zhemu.alterego.model.dto.agent.AgentAvatarGenerateRequest;
import org.zhemu.alterego.model.dto.agent.AgentCreateRequest;
import org.zhemu.alterego.model.vo.AgentVO;
import org.zhemu.alterego.service.AgentService;
import org.zhemu.alterego.util.UserContext;

/**
 * Agent控制器
 * @author: lushihao
 * @version: 1.0
 *           create: 2026-01-25 00:00
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/agent")
@Slf4j
@RequireLogin
@Tag(name = "Agent模块", description = "Agent创建、管理和互动接口")
public class AgentController {

    private final AgentService agentService;

    /**
     * 创建Agent
     *
     * @param request 创建请求
     * @return Agent VO
     */
    @PostMapping("/create")
    @Operation(summary = "创建Agent", description = "用户创建一个新的Agent，系统自动分配物种并生成名称")
    public BaseResponse<AgentVO> createAgent(@Valid @RequestBody AgentCreateRequest request) {
        // 从上下文获取当前用户ID（假设已经登录，通过拦截器设置）
        Long userId = UserContext.getCurrentUserId();
        log.info("用户 {} 创建Agent，宠物名称:{},性格描述: {}", userId, request.getAgentName(), request.getPersonality());

        AgentVO agentVO = agentService.createAgent(userId, request);

        log.info("Agent创建成功: id={}, name={}", agentVO.getId(), agentVO.getAgentName());
        return ResultUtils.success(agentVO);
    }

    /**
     * 获取我的Agent
     *
     * @return Agent VO
     */
    @GetMapping("/my")
    @Operation(summary = "获取我的Agent", description = "获取当前登录用户的Agent信息")
    public BaseResponse<AgentVO> getMyAgent() {
        Long userId = UserContext.getCurrentUserId();
        AgentVO agentVO = agentService.getAgentByUserId(userId);
        return ResultUtils.success(agentVO);
    }

    /**
     * 触发生成 Agent 头像
     */
    @PostMapping("/avatar/generate")
    @Operation(summary = "生成Agent头像", description = "异步生成头像，返回是否提交成功")
    public BaseResponse<Boolean> generateAvatar(@Valid @RequestBody AgentAvatarGenerateRequest request) {
        Long userId = UserContext.getCurrentUserId();
        boolean result = agentService.generateAvatar(userId, request.getAgentId());
        return ResultUtils.success(result);
    }
}

