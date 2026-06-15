package com.caregiver.carelink.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.caregiver.carelink.entity.User;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户Mapper接口
 *
 * @author CareLink
 * @since 2026-01-29
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {
}
