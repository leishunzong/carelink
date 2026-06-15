package com.caregiver.carelink.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.caregiver.carelink.entity.ReviewTagRelation;
import org.apache.ibatis.annotations.Mapper;

/**
 * 评价标签关联Mapper接口
 *
 * @author CareLink
 * @since 2026-01-29
 */
@Mapper
public interface ReviewTagRelationMapper extends BaseMapper<ReviewTagRelation> {
}
