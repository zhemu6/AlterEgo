package org.zhemu.alterego.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.zhemu.alterego.common.BaseResponse;
import org.zhemu.alterego.common.ResultUtils;
import org.zhemu.alterego.exception.ErrorCode;
import org.zhemu.alterego.exception.ThrowUtils;
import org.zhemu.alterego.model.dto.pk.PkCreateRequest;
import org.zhemu.alterego.model.dto.pk.PkQueryRequest;
import org.zhemu.alterego.model.dto.pk.PkVoteRequest;
import org.zhemu.alterego.model.vo.PkPostVO;
import org.zhemu.alterego.service.PkService;
import org.zhemu.alterego.util.UserContext;

/**
 * PK 投票 Controller
 *
 * @author lushihao
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/pk")
@Slf4j
public class PkVoteController {

    private final PkService pkService;

    /**
     * Agent 发起 PK
     */
    @PostMapping("/create")
    public BaseResponse<PkPostVO> createPk(
            @RequestBody @Validated PkCreateRequest request) {
        Long userId = UserContext.getCurrentUserId();
        ThrowUtils.throwIf(userId == null, ErrorCode.NOT_LOGIN_ERROR);
        PkPostVO result = pkService.createPk(request, userId);
        return ResultUtils.success(result);
    }

    /**
     * Agent 投票
     */
    @PostMapping("/vote")
    public BaseResponse<PkPostVO> vote(
            @RequestBody @Validated PkVoteRequest request) {
        Long userId = UserContext.getCurrentUserId();
        ThrowUtils.throwIf(userId == null, ErrorCode.NOT_LOGIN_ERROR);
        PkPostVO result = pkService.vote(request, userId);
        return ResultUtils.success(result);
    }

    /**
     * 分页查询 PK 列表
     */
    @PostMapping("/list/page")
    public BaseResponse<Page<PkPostVO>> listPkByPage(
            @RequestBody PkQueryRequest request) {
        Long userId = UserContext.getCurrentUserId();
        // userId 可为空（未登录也能查看列表）
        Page<PkPostVO> result = pkService.listPkByPage(request, userId);
        return ResultUtils.success(result);
    }

    /**
     * 获取 PK 详情
     */
    @GetMapping("/get")
    public BaseResponse<PkPostVO> getPkById(
            @RequestParam Long postId) {
        Long userId = UserContext.getCurrentUserId();
        // userId 可为空（未登录也能查看详情）
        PkPostVO result = pkService.getPkById(postId, userId);
        return ResultUtils.success(result);
    }
}
