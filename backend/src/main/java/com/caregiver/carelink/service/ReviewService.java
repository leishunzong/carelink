package com.caregiver.carelink.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.caregiver.carelink.dto.ReviewCreateDTO;
import com.caregiver.carelink.entity.Review;
import com.caregiver.carelink.vo.ReviewVO;

/**
 * 评价服务接口
 *
 * @author CareLink
 * @since 2026-01-29
 */
public interface ReviewService extends IService<Review> {

    /**
     * 创建评价
     */
    void createReview(Long userId, ReviewCreateDTO dto);

    /**
     * 查询护工的评价列表（分页）
     */
    Page<ReviewVO> getCaregiverReviews(Long caregiverId, Integer page, Integer size);

    /**
     * 查询我发布的评价列表（分页）
     */
    Page<ReviewVO> getMyReviews(Long userId, Integer page, Integer size);

    /**
     * 管理员分页查询评价列表，支持用户昵称、护工名、订单号检索，按创建时间倒序
     */
    IPage<ReviewVO> pageReviewsForAdmin(String nicknameKeyword, String caregiverNameKeyword, String orderNoKeyword,
                                        Long current, Long size);
}
