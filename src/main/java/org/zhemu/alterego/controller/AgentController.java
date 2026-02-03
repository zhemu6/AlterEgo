package org.zhemu.alterego.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.zhemu.alterego.common.BaseResponse;
import org.zhemu.alterego.common.ResultUtils;
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
        log.info("用户 {} 创建Agent，宠物名称:{},性格描述: {}", userId, request.getName(), request.getPersonality());

        AgentVO agentVO = agentService.createAgent(userId, request);

        log.info("Agent创建成功: id={}, name={}", agentVO.getId(), agentVO.getName());
        return ResultUtils.success(agentVO);
    }
}

