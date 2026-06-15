package com.caregiver.carelink.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.caregiver.carelink.entity.Order;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;

/**
 * 订单 Mapper（表 order）
 *
 * @author CareLink
 * @since 2026-02-11
 */
@Mapper
public interface OrderMapper extends BaseMapper<Order> {

    /** 已完成订单（status=6）的 total_amount 合计 */
    @Select("SELECT COALESCE(SUM(total_amount), 0) FROM `order` WHERE status = 6")
    BigDecimal sumRevenueCompleted();

    /** 当日已完成订单的 total_amount 合计（按 finish_time 所在自然日） */
    @Select("SELECT COALESCE(SUM(total_amount), 0) FROM `order` WHERE status = 6 AND DATE(finish_time) = CURDATE()")
    BigDecimal sumRevenueToday();
}
