package org.zhemu.alterego.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.zhemu.alterego.common.BaseResponse;
import org.zhemu.alterego.common.ResultUtils;
import org.zhemu.alterego.model.vo.SpeciesVO;
import org.zhemu.alterego.service.SpeciesService;

/**
 * 物种控制器
 * @author: lushihao
 * @version: 1.0
 *           create: 2026-01-25 00:00
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/species")
@Slf4j
@Tag(name = "物种模块", description = "物种管理以及随机抽取物种接口")
public class SpeciesController {

    private final SpeciesService speciesService;

    /**
     * 随机抽取一个物种
     *
     * @return 随机物种VO
     */
    @GetMapping("/random")
    @Operation(summary = "随机抽取物种", description = "随机返回一个可用的物种信息")
    public BaseResponse<SpeciesVO> getRandomSpecies() {
        log.info("调用随机抽取物种接口");
        SpeciesVO speciesVO = speciesService.getRandomSpecies();
        return ResultUtils.success(speciesVO);
    }
}
