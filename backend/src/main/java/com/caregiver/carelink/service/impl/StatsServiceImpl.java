package com.caregiver.carelink.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.caregiver.carelink.entity.CaregiverStats;
import com.caregiver.carelink.entity.ReviewTagStats;
import com.caregiver.carelink.mapper.CaregiverStatsMapper;
import com.caregiver.carelink.mapper.ReviewTagStatsMapper;
import com.caregiver.carelink.service.StatsService;
import com.caregiver.carelink.vo.CaregiverStatsVO;
import com.caregiver.carelink.vo.TagCountVO;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * 统计服务实现类
 *
 * @author CareLink
 * @since 2026-01-29
 */
@Service
public class StatsServiceImpl implements StatsService {

    @Resource
    private CaregiverStatsMapper caregiverStatsMapper;

    @Resource
    private ReviewTagStatsMapper tagStatsMapper;

    @Override
    public CaregiverStatsVO getCaregiverStats(Long caregiverId) {
        CaregiverStatsVO vo = new CaregiverStatsVO();
        vo.setCaregiverId(caregiverId);

        // 查询基础统计
        CaregiverStats stats = caregiverStatsMapper.selectOne(
                new LambdaQueryWrapper<CaregiverStats>()
                        .eq(CaregiverStats::getCaregiverId, caregiverId)
        );

        if (stats != null) {
            vo.setOrderCount(stats.getOrderCount());
            vo.setReviewCount(stats.getReviewCount());
            vo.setStarCount(stats.getStarCount());
            vo.setGoodReviewRate(stats.getGoodReviewRate());
            vo.setCancelCount(stats.getCancelCount());
            // 平均星级 = 星级总和 / 评价数（保留1位小数）
            if (stats.getReviewCount() != null && stats.getReviewCount() > 0
                    && stats.getStarRatingSum() != null && stats.getStarRatingSum() > 0) {
                vo.setAverageRating(new BigDecimal(stats.getStarRatingSum())
                        .divide(BigDecimal.valueOf(stats.getReviewCount()), 1, RoundingMode.HALF_UP));
            } else {
                vo.setAverageRating(BigDecimal.ZERO);
            }
        } else {
            vo.setOrderCount(0);
            vo.setReviewCount(0);
            vo.setStarCount(0);
            vo.setGoodReviewRate(BigDecimal.ZERO);
            vo.setAverageRating(BigDecimal.ZERO);
            vo.setCancelCount(0);
        }

        // 查询标签统计
        List<TagCountVO> tagStats = getCaregiverTagStats(caregiverId);
        vo.setTagStats(tagStats);

        return vo;
    }

    @Override
    public List<TagCountVO> getCaregiverTagStats(Long caregiverId) {
        // 查询标签统计（标签名和类型已冗余在统计表中，无需再查标签表）
        LambdaQueryWrapper<ReviewTagStats> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ReviewTagStats::getCaregiverId, caregiverId)
                .orderByDesc(ReviewTagStats::getCount);
        List<ReviewTagStats> statsList = tagStatsMapper.selectList(wrapper);

        List<TagCountVO> result = new ArrayList<>();
        for (ReviewTagStats stats : statsList) {
            TagCountVO vo = TagCountVO.builder()
                    .tagId(stats.getTagId())
                    .tagName(stats.getTagName())
                    .tagType(stats.getTagType())
                    .count(stats.getCount())
                    .build();
            result.add(vo);
        }

        return result;
    }
}
