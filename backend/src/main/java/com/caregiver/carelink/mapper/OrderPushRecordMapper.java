package com.caregiver.carelink.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.caregiver.carelink.entity.OrderPushRecord;
import org.apache.ibatis.annotations.Mapper;

/**
 * 订单推送记录 Mapper
 *
 * @author CareLink
 * @since 2026-02-11
 */
@Mapper
public interface OrderPushRecordMapper extends BaseMapper<OrderPushRecord> {
}
