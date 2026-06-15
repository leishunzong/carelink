package com.caregiver.carelink.controller;

import com.caregiver.carelink.common.result.Result;
import com.caregiver.carelink.entity.ReviewTag;
import com.caregiver.carelink.service.ReviewTagService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

/**
 * 评价标签管理控制器
 *
 * @author CareLink
 * @since 2026-01-29
 */
@Api(tags = "标签管理")
@RestController
@RequestMapping("/tag")
public class ReviewTagController {

    @Resource
    private ReviewTagService reviewTagService;

    @ApiOperation("查询所有评价标签（公开接口）")
    @GetMapping("/list")
    public Result<List<ReviewTag>> getAllTags() {
        List<ReviewTag> tags = reviewTagService.getAllTags();
        return Result.success(tags);
    }

    @ApiOperation("按类型查询评价标签（公开接口）")
    @GetMapping("/list/{type}")
    public Result<List<ReviewTag>> getTagsByType(
            @ApiParam(value = "标签类型：1-好评, 2-差评", required = true) @PathVariable Integer type) {
        List<ReviewTag> tags = reviewTagService.getTagsByType(type);
        return Result.success(tags);
    }
}
