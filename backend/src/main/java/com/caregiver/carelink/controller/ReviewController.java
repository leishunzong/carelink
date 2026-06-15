package com.caregiver.carelink.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.caregiver.carelink.common.context.CaregiverContextHolder;
import com.caregiver.carelink.common.context.UserContextHolder;
import com.caregiver.carelink.common.result.PageResult;
import com.caregiver.carelink.common.result.Result;
import com.caregiver.carelink.dto.ReviewCreateDTO;
import com.caregiver.carelink.service.AiSummaryService;
import com.caregiver.carelink.service.ReviewService;
import com.caregiver.carelink.vo.ReviewVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

/**
 * 评价管理控制器
 *
 * @author CareLink
 * @since 2026-01-29
 */
@Api(tags = "评价管理")
@RestController
@RequestMapping("/review")
public class ReviewController {

    @Resource
    private ReviewService reviewService;

    @Resource
    private AiSummaryService aiSummaryService;

    // ==================== 用户端接口 ====================

    @ApiOperation("创建评价（用户）")
    @PostMapping("/user/create")
    public Result<Void> createReview(@Validated @RequestBody ReviewCreateDTO dto) {
        Long userId = UserContextHolder.getUserId();
        reviewService.createReview(userId, dto);
        return Result.success("评价成功");
    }

    @ApiOperation("查询我发布的评价列表（用户）")
    @GetMapping("/user/my-list")
    public Result<PageResult<ReviewVO>> getMyReviews(
            @ApiParam("页码") @RequestParam(defaultValue = "1") Integer page,
            @ApiParam("每页数量") @RequestParam(defaultValue = "10") Integer size) {
        Long userId = UserContextHolder.getUserId();
        Page<ReviewVO> reviewPage = reviewService.getMyReviews(userId, page, size);
        return Result.success(PageResult.of(reviewPage));
    }

    @ApiOperation("查询护工的评价列表（用户查看护工详情）")
    @GetMapping("/user/caregiver/{caregiverId}")
    public Result<PageResult<ReviewVO>> getCaregiverReviews(
            @ApiParam(value = "护工ID", required = true) @PathVariable Long caregiverId,
            @ApiParam("页码") @RequestParam(defaultValue = "1") Integer page,
            @ApiParam("每页数量") @RequestParam(defaultValue = "10") Integer size) {
        Page<ReviewVO> reviewPage = reviewService.getCaregiverReviews(caregiverId, page, size);
        return Result.success(PageResult.of(reviewPage));
    }

    // ==================== 护工端接口 ====================

    @ApiOperation("查询我的评价列表（护工）")
    @GetMapping("/caregiver/my-list")
    public Result<PageResult<ReviewVO>> getMyReviewsAsCaregiver(
            @ApiParam("页码") @RequestParam(defaultValue = "1") Integer page,
            @ApiParam("每页数量") @RequestParam(defaultValue = "10") Integer size) {
        Long caregiverId = CaregiverContextHolder.getCaregiverId();
        Page<ReviewVO> reviewPage = reviewService.getCaregiverReviews(caregiverId, page, size);
        return Result.success(PageResult.of(reviewPage));
    }

    // ==================== AI 智能评价摘要 ====================

    @ApiOperation("AI智能评价摘要（根据护工所有评价生成AI总结）")
    @GetMapping("/user/caregiver/{caregiverId}/summary")
    public Result<String> getCaregiverReviewSummary(
            @ApiParam(value = "护工ID", required = true) @PathVariable Long caregiverId) {
        String summary = aiSummaryService.getCaregiverReviewSummary(caregiverId);
        return Result.success("操作成功", summary);
    }
}
