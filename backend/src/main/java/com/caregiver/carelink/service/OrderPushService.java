package com.caregiver.carelink.service;

import com.caregiver.carelink.entity.Order;

import java.util.List;

/**
 * 订单推送服务
 *
 * @author CareLink
 * @since 2026-02-11
 */
public interface OrderPushService {

    /**
     * 将订单推送给一批护工
     *
     * @param order       订单（非 null）
     * @param caregiverIds 要推送的护工 ID 列表，可为空列表
     */
    void pushOrderToCaregivers(Order order, List<Long> caregiverIds);
}
