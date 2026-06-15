package com.caregiver.carelink.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.caregiver.carelink.entity.Admin;
import org.apache.ibatis.annotations.Mapper;

/**
 * 管理员 Mapper
 *
 * @author CareLink
 * @since 2026-02-24
 */
@Mapper
public interface AdminMapper extends BaseMapper<Admin> {
}
