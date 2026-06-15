package com.caregiver.carelink.service;

/**
 * AI 摘要服务：评价摘要 + 智能护理方案推荐
 *
 * @author CareLink
 * @since 2026-03-28
 */
public interface AiSummaryService {

    /**
     * 获取护工评价 AI 摘要（优先取缓存，无缓存则调用 LLM 生成并缓存）
     *
     * @param caregiverId 护工ID
     * @return AI 生成的评价摘要文本
     */
    String getCaregiverReviewSummary(Long caregiverId);

    /**
     * 根据用户描述的老人状况，智能推荐服务包和护理方案
     *
     * @param description 用户对老人状况的描述（如"我爸78岁，有轻度阿尔茨海默，行动不便"）
     * @return AI 生成的护理方案推荐文本
     */
    String generateCareRecommendation(String description);

    /**
     * 异步刷新护工评价 AI 摘要（新评价产生时调用，后台异步生成并更新缓存，避免用户查看时阻塞）
     *
     * @param caregiverId 护工ID
     */
    void refreshCaregiverReviewSummaryAsync(Long caregiverId);
}
