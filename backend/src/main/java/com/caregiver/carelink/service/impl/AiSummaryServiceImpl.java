package com.caregiver.carelink.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.caregiver.carelink.entity.Caregiver;
import com.caregiver.carelink.entity.CaregiverStats;
import com.caregiver.carelink.entity.Review;
import com.caregiver.carelink.entity.ServicePackage;
import com.caregiver.carelink.mapper.CaregiverMapper;
import com.caregiver.carelink.mapper.CaregiverStatsMapper;
import com.caregiver.carelink.mapper.ReviewMapper;
import com.caregiver.carelink.service.AiSummaryService;
import com.caregiver.carelink.service.ServicePackageService;
import com.caregiver.carelink.utils.RedisUtils;
import com.caregiver.carelink.vo.ServicePackageVO;
import dev.langchain4j.model.chat.ChatModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.util.List;
import java.util.stream.Collectors;

/**
 * AI 摘要服务实现
 *
 * @author CareLink
 * @since 2026-03-28
 */
@Service
public class AiSummaryServiceImpl implements AiSummaryService {

    private static final Logger log = LoggerFactory.getLogger(AiSummaryServiceImpl.class);

    /** 评价摘要 Redis 缓存前缀 */
    private static final String REVIEW_SUMMARY_KEY_PREFIX = "ai:review:summary:";
    /** 评价摘要缓存过期时间（秒），24 小时 */
    private static final long REVIEW_SUMMARY_TTL = 86400L;

    @Autowired(required = false)
    private ChatModel chatModel;

    @Resource
    private ReviewMapper reviewMapper;

    @Resource
    private CaregiverMapper caregiverMapper;

    @Resource
    private CaregiverStatsMapper caregiverStatsMapper;

    @Resource
    private ServicePackageService servicePackageService;

    @Resource
    private RedisUtils redisUtils;

    @Override
    public String getCaregiverReviewSummary(Long caregiverId) {
        if (!isAiModelEnabled()) {
            return aiDisabledMessage();
        }
        if (caregiverId == null) {
            return "护工ID不能为空";
        }

        // 优先取缓存
        String cacheKey = REVIEW_SUMMARY_KEY_PREFIX + caregiverId;
        Object cached = redisUtils.get(cacheKey);
        if (cached != null) {
            log.info("护工评价摘要命中缓存: caregiverId={}", caregiverId);
            return cached.toString();
        }

        // 查询护工基本信息
        Caregiver caregiver = caregiverMapper.selectById(caregiverId);
        if (caregiver == null) {
            return "未找到该护工";
        }

        // 查询护工统计数据
        CaregiverStats stats = caregiverStatsMapper.selectOne(
                new LambdaQueryWrapper<CaregiverStats>()
                        .eq(CaregiverStats::getCaregiverId, caregiverId));

        // 查询最近 20 条评价
        Page<Review> page = reviewMapper.selectPage(
                new Page<>(1, 20),
                new LambdaQueryWrapper<Review>()
                        .eq(Review::getCaregiverId, caregiverId)
                        .orderByDesc(Review::getCreateTime));

        List<Review> reviews = page.getRecords();
        if (reviews == null || reviews.isEmpty()) {
            String result = caregiver.getRealName() + "暂无用户评价。";
            redisUtils.set(cacheKey, result, REVIEW_SUMMARY_TTL);
            return result;
        }

        // 拼接评价文本作为 prompt 输入
        StringBuilder reviewTexts = new StringBuilder();
        for (Review r : reviews) {
            String typeLabel = (r.getType() != null && r.getType() == 1) ? "好评" : "差评";
            reviewTexts.append("- ").append(typeLabel);
            if (r.getStars() != null) {
                reviewTexts.append(" ").append(r.getStars()).append("星");
            }
            if (StringUtils.hasText(r.getContent())) {
                reviewTexts.append("：").append(r.getContent());
            }
            reviewTexts.append("\n");
        }

        String statsInfo = "";
        if (stats != null) {
            statsInfo = String.format("累计完成%d单，好评率%.1f%%，累计评价%d条。",
                    stats.getOrderCount() != null ? stats.getOrderCount() : 0,
                    stats.getGoodReviewRate() != null ? stats.getGoodReviewRate().doubleValue() : 0,
                    stats.getReviewCount() != null ? stats.getReviewCount() : 0);
        }

        String prompt = String.format(
                "你是一个专业的护理服务评价分析师。请根据以下用户评价，为护工「%s」生成一段简洁的评价摘要（100-200字）。\n" +
                "要求：\n" +
                "1. 总结该护工的主要优点和不足\n" +
                "2. 提炼高频出现的关键词\n" +
                "3. 用客观、中立的语气\n" +
                "4. 直接输出摘要内容，不要加标题或前缀\n\n" +
                "护工统计：%s\n" +
                "用户评价（共%d条）：\n%s",
                caregiver.getRealName(), statsInfo, reviews.size(), reviewTexts);

        try {
            String summary = chatModel.chat(prompt);
            log.info("护工评价摘要生成成功: caregiverId={}, 摘要长度={}", caregiverId, summary.length());
            redisUtils.set(cacheKey, summary, REVIEW_SUMMARY_TTL);
            return summary;
        } catch (Exception e) {
            log.error("护工评价摘要生成失败: caregiverId={}", caregiverId, e);
            return "评价摘要暂时无法生成，请稍后再试。";
        }
    }

    @Override
    public String generateCareRecommendation(String description) {
        if (!isAiModelEnabled()) {
            return aiDisabledMessage();
        }
        if (!StringUtils.hasText(description)) {
            return "请描述老人的基本情况，如年龄、健康状况、自理能力等，以便为您推荐合适的护理方案。";
        }

        // 查询所有上架的服务包
        List<ServicePackage> packages = servicePackageService.list(
                new LambdaQueryWrapper<ServicePackage>()
                        .eq(ServicePackage::getStatus, 1));

        if (packages == null || packages.isEmpty()) {
            return "暂无可用的服务包，请稍后再试。";
        }

        // 拼接服务包信息（结构化格式，包含完整的计费方式和价格）
        StringBuilder packageInfo = new StringBuilder();
        for (ServicePackage pkg : packages) {
            packageInfo.append("- 【ID:").append(pkg.getId()).append("】")
                    .append(pkg.getName())
                    .append("（").append(categoryName(pkg.getCategory())).append("）");
            if (StringUtils.hasText(pkg.getDescription())) {
                packageInfo.append("：").append(pkg.getDescription());
            }
            // 列出所有支持的计费方式和价格
            packageInfo.append(" | 支持计费：");
            boolean hasPrice = false;
            if (pkg.getPriceMonth() != null) {
                packageInfo.append("按月").append(pkg.getPriceMonth()).append("元(billingMethod=1)");
                hasPrice = true;
            }
            if (pkg.getPriceDay() != null) {
                if (hasPrice) packageInfo.append("、");
                packageInfo.append("按天").append(pkg.getPriceDay()).append("元(billingMethod=2)");
                hasPrice = true;
            }
            if (pkg.getPriceHour() != null) {
                if (hasPrice) packageInfo.append("、");
                packageInfo.append("按小时").append(pkg.getPriceHour()).append("元(billingMethod=3)");
                hasPrice = true;
            }
            if (pkg.getPriceTimes() != null) {
                if (hasPrice) packageInfo.append("、");
                packageInfo.append("按次").append(pkg.getPriceTimes()).append("元(billingMethod=4)");
            }
            if (StringUtils.hasText(pkg.getMandatorySkills())) {
                packageInfo.append(" | 需要技能:").append(pkg.getMandatorySkills());
            }
            packageInfo.append("\n");
        }

        String prompt = String.format(
                "你是一个专业的居家养老护理顾问。用户描述了老人的情况，请根据描述推荐最合适的服务包和护理方案。\n\n" +
                "用户描述：%s\n\n" +
                "平台可选服务包：\n%s\n" +
                "要求：\n" +
                "1. 推荐 1-2 个最适合的服务包，说明推荐理由\n" +
                "2. 每个推荐的服务包必须按以下格式输出关键信息：\n" +
                "   - 服务包名称（ID:xxx）\n" +
                "   - 推荐计费方式及对应价格\n" +
                "   - 推荐理由\n" +
                "3. 给出护理注意事项和建议\n" +
                "4. 对护工的技能要求建议\n" +
                "5. 用温暖专业的语气，简洁明了\n" +
                "6. 在最后加上一句引导语：\"如果您觉得合适，告诉我您想选择哪个服务包，我可以直接帮您下单预约。\"",
                description, packageInfo);

        try {
            String recommendation = chatModel.chat(prompt);
            log.info("智能护理推荐生成成功: 描述长度={}, 推荐长度={}", description.length(), recommendation.length());
            return recommendation;
        } catch (Exception e) {
            log.error("智能护理推荐生成失败", e);
            return "护理方案推荐暂时无法生成，请稍后再试。";
        }
    }

    @Override
    @org.springframework.scheduling.annotation.Async("taskExecutor")
    public void refreshCaregiverReviewSummaryAsync(Long caregiverId) {
        if (!isAiModelEnabled()) {
            log.info("AI 模型未启用，跳过异步刷新护工评价摘要: caregiverId={}", caregiverId);
            return;
        }
        log.info("异步刷新护工评价摘要开始: caregiverId={}", caregiverId);
        try {
            // 不删除旧缓存，让用户在新摘要生成期间仍可查到旧摘要
            // 先清除缓存标记，使 getCaregiverReviewSummary 跳过缓存重新生成
            String cacheKey = REVIEW_SUMMARY_KEY_PREFIX + caregiverId;

            // 直接调用 LLM 生成新摘要并覆盖写入缓存（getCaregiverReviewSummary 内部会命中旧缓存，所以这里需要独立生成）
            Caregiver caregiver = caregiverMapper.selectById(caregiverId);
            if (caregiver == null) {
                log.warn("异步刷新摘要时护工不存在: caregiverId={}", caregiverId);
                return;
            }

            CaregiverStats stats = caregiverStatsMapper.selectOne(
                    new LambdaQueryWrapper<CaregiverStats>()
                            .eq(CaregiverStats::getCaregiverId, caregiverId));

            Page<Review> page = reviewMapper.selectPage(
                    new Page<>(1, 20),
                    new LambdaQueryWrapper<Review>()
                            .eq(Review::getCaregiverId, caregiverId)
                            .orderByDesc(Review::getCreateTime));

            List<Review> reviews = page.getRecords();
            if (reviews == null || reviews.isEmpty()) {
                String result = caregiver.getRealName() + "暂无用户评价。";
                redisUtils.set(cacheKey, result, REVIEW_SUMMARY_TTL);
                return;
            }

            StringBuilder reviewTexts = new StringBuilder();
            for (Review r : reviews) {
                String typeLabel = (r.getType() != null && r.getType() == 1) ? "好评" : "差评";
                reviewTexts.append("- ").append(typeLabel);
                if (r.getStars() != null) {
                    reviewTexts.append(" ").append(r.getStars()).append("星");
                }
                if (StringUtils.hasText(r.getContent())) {
                    reviewTexts.append("：").append(r.getContent());
                }
                reviewTexts.append("\n");
            }

            String statsInfo = "";
            if (stats != null) {
                statsInfo = String.format("累计完成%d单，好评率%.1f%%，累计评价%d条。",
                        stats.getOrderCount() != null ? stats.getOrderCount() : 0,
                        stats.getGoodReviewRate() != null ? stats.getGoodReviewRate().doubleValue() : 0,
                        stats.getReviewCount() != null ? stats.getReviewCount() : 0);
            }

            String prompt = String.format(
                    "你是一个专业的护理服务评价分析师。请根据以下用户评价，为护工「%s」生成一段简洁的评价摘要（100-200字）。\n" +
                    "要求：\n" +
                    "1. 总结该护工的主要优点和不足\n" +
                    "2. 提炼高频出现的关键词\n" +
                    "3. 用客观、中立的语气\n" +
                    "4. 直接输出摘要内容，不要加标题或前缀\n\n" +
                    "护工统计：%s\n" +
                    "用户评价（共%d条）：\n%s",
                    caregiver.getRealName(), statsInfo, reviews.size(), reviewTexts);

            String summary = chatModel.chat(prompt);
            // 新摘要生成成功后直接覆盖旧缓存
            redisUtils.set(cacheKey, summary, REVIEW_SUMMARY_TTL);
            log.info("异步刷新护工评价摘要完成: caregiverId={}, 摘要长度={}", caregiverId, summary.length());
        } catch (Exception e) {
            log.error("异步刷新护工评价摘要失败（旧缓存仍保留可用）: caregiverId={}", caregiverId, e);
        }
    }

    private String categoryName(Integer category) {
        if (category == null) {
            return "其他";
        }
        switch (category) {
            case 1: return "居家陪护";
            case 2: return "医院陪护";
            case 3: return "周期护理";
            case 4: return "家政服务";
            case 5: return "陪诊服务";
            case 6: return "母婴护理";
            default: return "其他";
        }
    }

    private boolean isAiModelEnabled() {
        return chatModel != null;
    }

    private String aiDisabledMessage() {
        return "AI助手未启用，请配置 AI_MODEL_ENABLED=true 和 AI_API_KEY 后重试。";
    }
}
