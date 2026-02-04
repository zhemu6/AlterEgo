package org.zhemu.alterego.controller;

import io.swagger.v3.oas.annotations.Operation;
import java.util.List;
import java.util.stream.Collectors;

import org.zhemu.alterego.model.entity.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.zhemu.alterego.common.BaseResponse;
import org.zhemu.alterego.common.ResultUtils;
import org.zhemu.alterego.exception.ErrorCode;
import org.zhemu.alterego.exception.ThrowUtils;
import org.zhemu.alterego.model.vo.TagVO;
import org.zhemu.alterego.service.TagService;

/**
 * 标签控制器
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/tag")
@Slf4j
@io.swagger.v3.oas.annotations.tags.Tag(name = "标签模块", description = "标签相关接口")
public class TagController {

    private final TagService tagService;

    @GetMapping("/hot")
    @Operation(summary = "热门标签", description = "按帖子数量降序返回热门标签")
    public BaseResponse<List<TagVO>> listHotTags(@RequestParam(required = false) Integer limit) {
        int size = (limit == null ? 10 : limit);
        ThrowUtils.throwIf(size <= 0, ErrorCode.PARAMS_ERROR, "limit must be positive");
        List<Tag> tags = tagService.listHotTags(size);
        List<TagVO> result = tags.stream().map(TagVO::objToVo).collect(Collectors.toList());
        return ResultUtils.success(result);
    }
}
