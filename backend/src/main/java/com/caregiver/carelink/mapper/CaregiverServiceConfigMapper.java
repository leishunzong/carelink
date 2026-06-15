package com.caregiver.carelink.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.caregiver.carelink.entity.CaregiverServiceConfig;
import org.apache.ibatis.annotations.Mapper;

/**
 * 护工服务包准入关联 Mapper
 *
 * @author CareLink
 * @since 2026-02-11
 */
@Mapper
public interface CaregiverServiceConfigMapper extends BaseMapper<CaregiverServiceConfig> {
}
