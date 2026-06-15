package com.caregiver.carelink.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.caregiver.carelink.entity.CaregiverStats;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 护工统计Mapper接口
 *
 * @author CareLink
 * @since 2026-01-29
 */
@Mapper
public interface CaregiverStatsMapper extends BaseMapper<CaregiverStats> {

    /**
     * 护工完单量 +1（原子操作：无行则插入 1，有行则 order_count+1，并发安全无需应用层加锁）
     */
    @Insert("INSERT INTO caregiver_stats (caregiver_id, order_count, review_count, star_count, cancel_count, good_review_rate) " +
            "VALUES (#{caregiverId}, 1, 0, 0, 0, 100) " +
            "ON DUPLICATE KEY UPDATE order_count = IFNULL(order_count, 0) + 1, update_time = CURRENT_TIMESTAMP")
    void incrementOrderCount(@Param("caregiverId") Long caregiverId);

    /**
     * 护工爽约/取消次数 +1（原子操作：无行则插入，有行则 cancel_count+1，用于用户取消待上门订单时累加护工爽约次数）
     */
    @Insert("INSERT INTO caregiver_stats (caregiver_id, order_count, review_count, star_count, cancel_count, good_review_rate) " +
            "VALUES (#{caregiverId}, 0, 0, 0, 1, 100) " +
            "ON DUPLICATE KEY UPDATE cancel_count = IFNULL(cancel_count, 0) + 1, update_time = CURRENT_TIMESTAMP")
    void incrementCancelCount(@Param("caregiverId") Long caregiverId);
}
