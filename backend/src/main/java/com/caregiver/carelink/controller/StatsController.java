package com.caregiver.carelink.controller;

import com.caregiver.carelink.common.context.CaregiverContextHolder;
import com.caregiver.carelink.common.context.UserContextHolder;
import com.caregiver.carelink.common.result.Result;
import com.caregiver.carelink.service.StatsService;
import com.caregiver.carelink.vo.CaregiverStatsVO;
import com.caregiver.carelink.vo.TagCountVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

/**
 * 统计数据控制器
 *
 * @author CareLink
 * @since 2026-01-29
 */
@Api(tags = "统计数据")
@RestController
@RequestMapping("/stats")
public class StatsController {

    @Resource
    private StatsService statsService;

    // ==================== 用户端接口 ====================

    @ApiOperation("查询护工统计信息（用户查看护工详情）")
    @GetMapping("/user/caregiver/{caregiverId}")
    public Result<CaregiverStatsVO> getCaregiverStats(
            @ApiParam(value = "护工ID", required = true) @PathVariable Long caregiverId) {
        CaregiverStatsVO stats = statsService.getCaregiverStats(caregiverId);
        return Result.success(stats);
    }

    @ApiOperation("查询护工标签统计（用户查看护工详情）")
    @GetMapping("/user/caregiver/{caregiverId}/tags")
    public Result<List<TagCountVO>> getCaregiverTagStats(
            @ApiParam(value = "护工ID", required = true) @PathVariable Long caregiverId) {
        List<TagCountVO> tagStats = statsService.getCaregiverTagStats(caregiverId);
        return Result.success(tagStats);
    }

    // ==================== 护工端接口 ====================

    @ApiOperation("查询我的统计信息（护工）")
    @GetMapping("/caregiver/my")
    public Result<CaregiverStatsVO> getMyStats() {
        Long caregiverId = CaregiverContextHolder.getCaregiverId();
        CaregiverStatsVO stats = statsService.getCaregiverStats(caregiverId);
        return Result.success(stats);
    }

    @ApiOperation("查询我的标签统计（护工）")
    @GetMapping("/caregiver/my/tags")
    public Result<List<TagCountVO>> getMyTagStats() {
        Long caregiverId = CaregiverContextHolder.getCaregiverId();
        List<TagCountVO> tagStats = statsService.getCaregiverTagStats(caregiverId);
        return Result.success(tagStats);
    }
}
