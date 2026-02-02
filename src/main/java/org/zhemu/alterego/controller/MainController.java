package org.zhemu.alterego.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.zhemu.alterego.common.BaseResponse;
import org.zhemu.alterego.common.ResultUtils;

/**
 * 测试项目和接口文档是否能够正常生成
 * @author: lushihao
 * @version: 1.0
 * create:   2026-01-23   22:19
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/health")
public class MainController {

    /**
     * 健康检查
     */
    @GetMapping("/health")
    public BaseResponse<String> health() {
        return ResultUtils.success("ok");
    }
}

