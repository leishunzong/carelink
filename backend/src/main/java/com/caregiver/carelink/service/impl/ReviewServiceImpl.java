package com.caregiver.carelink.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.caregiver.carelink.common.exception.BusinessException;
import com.caregiver.carelink.common.result.ResultCode;
import com.caregiver.carelink.dto.ReviewCreateDTO;
import com.caregiver.carelink.entity.*;
import com.caregiver.carelink.mapper.*;
import com.caregiver.carelink.service.AiSummaryService;
import com.caregiver.carelink.service.ReviewService;
import com.caregiver.carelink.vo.ReviewVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 评价服务实现类
 *
 * @author CareLink
 * @since 2026-01-29
 */
@Service
public class ReviewServiceImpl extends ServiceImpl<ReviewMapper, Review> implements ReviewService {

    private static final Logger log = LoggerFactory.getLogger(ReviewServiceImpl.class);

    @Resource
    private UserMapper userMapper;

    @Resource
    private ReviewTagMapper reviewTagMapper;

    @Resource
    private ReviewTagRelationMapper relationMapper;

    @Resource
    private ReviewTagStatsMapper tagStatsMapper;

    @Resource
    private CaregiverStatsMapper caregiverStatsMapper;

    @Resource
    private OrderMapper orderMapper;

    @Resource
    private CaregiverMapper caregiverMapper;

    @Resource
    private com.caregiver.carelink.utils.RedisUtils redisUtils;

    @Resource
    private AiSummaryService aiSummaryService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createReview(Long userId, ReviewCreateDTO dto) {
        log.info("创建评价 userId={}, orderId={}, caregiverId={}, stars={}", userId, dto.getOrderId(), dto.getCaregiverId(), dto.getStars());
        // 校验订单ID必须存在
        if (dto.getOrderId() == null) {
            throw new BusinessException("订单ID不能为空，请从订单页面进入评价");
        }

        // 校验订单是否存在、归属、状态及是否已评价
        Order order = orderMapper.selectById(dto.getOrderId());
        if (order == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "订单不存在");
        }
        if (!order.getUserId().equals(userId)) {
            throw new BusinessException("无权评价该订单");
        }
        if (order.getStatus() == null || order.getStatus() != 6) {
            throw new BusinessException("仅支持对已完成的订单进行评价");
        }
        long existCount = count(new LambdaQueryWrapper<Review>().eq(Review::getOrderId, dto.getOrderId()));
        if (existCount > 0) {
            throw new BusinessException("该订单已评价，不可重复评价");
        }
        if (dto.getCaregiverId() != null && !dto.getCaregiverId().equals(order.getCaregiverId())) {
            throw new BusinessException("护工与订单不符");
        }

        // 获取用户信息（用于快照）
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "用户不存在");
        }

        // 创建评价（订单号、服务时间从订单表回填）
        Review review = new Review();
        review.setOrderId(dto.getOrderId());
        review.setOrderNo(order.getOrderNo());
        review.setServiceDate(order.getFinishTime());
        review.setUserId(userId);
        review.setCaregiverId(dto.getCaregiverId() != null ? dto.getCaregiverId() : order.getCaregiverId());
        review.setContent(dto.getContent());
        review.setType(dto.getType());
        review.setStars(dto.getStars());
        review.setIsAnonymous(dto.getIsAnonymous() != null ? dto.getIsAnonymous() : 0);

        // 如果不是匿名，保存用户快照
        if (review.getIsAnonymous() == 0) {
            review.setNickname(user.getNickname());
            review.setAvatar(user.getAvatar());
        }

        save(review);
        log.info("评价创建成功 reviewId={}, userId={}, orderId={}, caregiverId={}, type={}, stars={}",
                review.getId(), userId, dto.getOrderId(), review.getCaregiverId(), dto.getType(), dto.getStars());

        // 保存标签关联
        if (dto.getTagIds() != null && !dto.getTagIds().isEmpty()) {
            for (Long tagId : dto.getTagIds()) {
                ReviewTagRelation relation = new ReviewTagRelation();
                relation.setReviewId(review.getId());
                relation.setTagId(tagId);
                relationMapper.insert(relation);
            }
        }

        // 异步更新统计（含星级）
        updateCaregiverStatsAsync(dto.getCaregiverId(), dto.getType(), dto.getStars(), dto.getTagIds());

        // 异步刷新该护工的 AI 评价摘要（有新评价时后台重新生成并更新缓存，避免用户查看时阻塞）
        Long cgId = dto.getCaregiverId() != null ? dto.getCaregiverId() : order.getCaregiverId();
        if (cgId != null) {
            aiSummaryService.refreshCaregiverReviewSummaryAsync(cgId);
        }
    }

    @Override
    public Page<ReviewVO> getCaregiverReviews(Long caregiverId, Integer page, Integer size) {
        // 分页查询评价
        Page<Review> reviewPage = new Page<>(page, size);
        LambdaQueryWrapper<Review> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Review::getCaregiverId, caregiverId)
                .orderByDesc(Review::getCreateTime);
        page(reviewPage, wrapper);

        // 转换为VO
        Page<ReviewVO> voPage = new Page<>();
        BeanUtil.copyProperties(reviewPage, voPage, "records");
        
        List<ReviewVO> voList = new ArrayList<>();
        for (Review review : reviewPage.getRecords()) {
            ReviewVO vo = convertToVO(review);
            voList.add(vo);
        }
        voPage.setRecords(voList);

        return voPage;
    }

    @Override
    public Page<ReviewVO> getMyReviews(Long userId, Integer page, Integer size) {
        // 分页查询我发布的评价
        Page<Review> reviewPage = new Page<>(page, size);
        LambdaQueryWrapper<Review> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Review::getUserId, userId)
                .orderByDesc(Review::getCreateTime);
        page(reviewPage, wrapper);

        // 转换为VO
        Page<ReviewVO> voPage = new Page<>();
        BeanUtil.copyProperties(reviewPage, voPage, "records");
        
        List<ReviewVO> voList = new ArrayList<>();
        for (Review review : reviewPage.getRecords()) {
            ReviewVO vo = convertToVO(review);
            voList.add(vo);
        }
        voPage.setRecords(voList);

        return voPage;
    }

    @Override
    public IPage<ReviewVO> pageReviewsForAdmin(String nicknameKeyword, String caregiverNameKeyword, String orderNoKeyword,
                                                Long current, Long size) {
        List<Long> caregiverIds = null;
        if (StringUtils.hasText(caregiverNameKeyword)) {
            List<Caregiver> caregivers = caregiverMapper.selectList(
                    new LambdaQueryWrapper<Caregiver>().like(Caregiver::getRealName, caregiverNameKeyword).select(Caregiver::getId));
            caregiverIds = caregivers.stream().map(Caregiver::getId).collect(Collectors.toList());
            if (caregiverIds.isEmpty()) {
                Page<ReviewVO> emptyPage = new Page<>(current, size, 0L);
                emptyPage.setRecords(Collections.emptyList());
                return emptyPage;
            }
        }
        Page<Review> reviewPage = new Page<>(current, size);
        LambdaQueryWrapper<Review> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StringUtils.hasText(nicknameKeyword), Review::getNickname, nicknameKeyword)
                .like(StringUtils.hasText(orderNoKeyword), Review::getOrderNo, orderNoKeyword)
                .in(caregiverIds != null && !caregiverIds.isEmpty(), Review::getCaregiverId, caregiverIds)
                .orderByDesc(Review::getCreateTime);
        page(reviewPage, wrapper);
        Page<ReviewVO> voPage = new Page<>(reviewPage.getCurrent(), reviewPage.getSize(), reviewPage.getTotal());
        List<ReviewVO> voList = new ArrayList<>();
        for (Review review : reviewPage.getRecords()) {
            voList.add(convertToVO(review));
        }
        voPage.setRecords(voList);
        return voPage;
    }

    /**
     * 转换为VO
     */
    private ReviewVO convertToVO(Review review) {
        ReviewVO vo = new ReviewVO();
        BeanUtil.copyProperties(review, vo);

        // 处理匿名显示
        if (review.getIsAnonymous() == 1) {
            vo.setNickname("匿名用户");
            vo.setAvatar(null);
        }

        // 护工信息（用于“我的评价”列表展示护工姓名）
        if (review.getCaregiverId() != null) {
            Caregiver caregiver = caregiverMapper.selectById(review.getCaregiverId());
            if (caregiver != null) {
                vo.setCaregiverName(caregiver.getRealName());
            }
        }

        // 查询标签
        List<String> tags = getReviewTags(review.getId());
        vo.setTags(tags);

        return vo;
    }

    /**
     * 获取评价的标签列表
     */
    private List<String> getReviewTags(Long reviewId) {
        LambdaQueryWrapper<ReviewTagRelation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ReviewTagRelation::getReviewId, reviewId);
        List<ReviewTagRelation> relations = relationMapper.selectList(wrapper);

        if (relations.isEmpty()) {
            return new ArrayList<>();
        }

        List<Long> tagIds = relations.stream()
                .map(ReviewTagRelation::getTagId)
                .collect(Collectors.toList());

        List<ReviewTag> tags = reviewTagMapper.selectBatchIds(tagIds);
        return tags.stream()
                .map(ReviewTag::getName)
                .collect(Collectors.toList());
    }

    /**
     * 异步更新护工统计数据
     */
    @Async("taskExecutor")
    public void updateCaregiverStatsAsync(Long caregiverId, Integer reviewType, Integer stars, List<Long> tagIds) {
        try {
            log.info("开始异步更新护工{}的统计数据", caregiverId);

            // 更新基础统计（含星级汇总）
            updateBasicStats(caregiverId, reviewType, stars);

            // 更新标签统计
            if (tagIds != null && !tagIds.isEmpty()) {
                updateTagStats(caregiverId, tagIds);
            }

            log.info("护工{}的统计数据更新完成", caregiverId);
        } catch (Exception e) {
            log.error("更新护工{}统计数据失败", caregiverId, e);
        }
    }

    /**
     * 更新基础统计（累计评价数、好评数、好评率、星级总和）
     */
    private void updateBasicStats(Long caregiverId, Integer reviewType, Integer stars) {
        CaregiverStats stats = caregiverStatsMapper.selectOne(
                new LambdaQueryWrapper<CaregiverStats>()
                        .eq(CaregiverStats::getCaregiverId, caregiverId)
        );

        if (stats == null) {
            stats = new CaregiverStats();
            stats.setCaregiverId(caregiverId);
            stats.setOrderCount(0);
            stats.setReviewCount(0);
            stats.setStarCount(0);
            stats.setStarRatingSum(0);
            stats.setCancelCount(0);
            caregiverStatsMapper.insert(stats);
        }

        // 累计评价数+1
        stats.setReviewCount(stats.getReviewCount() + 1);

        // 如果是好评，好评数+1
        if (reviewType == 1) {
            stats.setStarCount(stats.getStarCount() + 1);
        }

        // 星级总和累加（1-5星）
        if (stars != null && stars >= 1 && stars <= 5) {
            int sum = stats.getStarRatingSum() == null ? 0 : stats.getStarRatingSum();
            stats.setStarRatingSum(sum + stars);
        }

        // 计算好评率
        if (stats.getReviewCount() > 0) {
            BigDecimal rate = new BigDecimal(stats.getStarCount())
                    .multiply(new BigDecimal("100"))
                    .divide(new BigDecimal(stats.getReviewCount()), 2, RoundingMode.HALF_UP);
            stats.setGoodReviewRate(rate);
        }

        caregiverStatsMapper.updateById(stats);
    }

    /**
     * 更新标签统计
     */
    private void updateTagStats(Long caregiverId, List<Long> tagIds) {
        // 批量查询标签信息，避免循环内逐条查询
        List<ReviewTag> tagList = reviewTagMapper.selectBatchIds(tagIds);
        Map<Long, ReviewTag> tagMap = tagList.stream()
                .collect(Collectors.toMap(ReviewTag::getId, t -> t));

        for (Long tagId : tagIds) {
            ReviewTagStats stats = tagStatsMapper.selectOne(
                    new LambdaQueryWrapper<ReviewTagStats>()
                            .eq(ReviewTagStats::getCaregiverId, caregiverId)
                            .eq(ReviewTagStats::getTagId, tagId)
            );

            if (stats == null) {
                stats = new ReviewTagStats();
                stats.setCaregiverId(caregiverId);
                stats.setTagId(tagId);
                stats.setCount(1);
                // 冗余写入标签名和类型
                ReviewTag tag = tagMap.get(tagId);
                if (tag != null) {
                    stats.setTagName(tag.getName());
                    stats.setTagType(tag.getType());
                }
                tagStatsMapper.insert(stats);
            } else {
                stats.setCount(stats.getCount() + 1);
                tagStatsMapper.updateById(stats);
            }
        }
    }
}
