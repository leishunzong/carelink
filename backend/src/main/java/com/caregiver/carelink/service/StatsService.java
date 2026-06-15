package com.caregiver.carelink.service;

import com.caregiver.carelink.vo.CaregiverStatsVO;
import com.caregiver.carelink.vo.TagCountVO;

import java.util.List;

/**
 * 统计服务接口
 *
 * @author CareLink
 * @since 2026-01-29
 */
public interface StatsService {

    /**
     * 查询护工统计信息（基础统计+标签统计）
     */
    CaregiverStatsVO getCaregiverStats(Long caregiverId);

    /**
     * 查询护工的标签统计列表
     */
    List<TagCountVO> getCaregiverTagStats(Long caregiverId);
}
