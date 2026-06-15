package com.caregiver.carelink.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.caregiver.carelink.entity.Review;
import org.apache.ibatis.annotations.Mapper;

/**
 * 评价Mapper接口
 *
 * @author CareLink
 * @since 2026-01-29
 */
@Mapper
public interface ReviewMapper extends BaseMapper<Review> {
}
