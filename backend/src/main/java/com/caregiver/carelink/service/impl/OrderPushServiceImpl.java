package com.caregiver.carelink.service.impl;

import com.caregiver.carelink.entity.Order;
import com.caregiver.carelink.service.OrderPushService;
import com.caregiver.carelink.vo.OrderPushVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.util.List;

/**
 * 订单推送实现：通过 WebSocket 将订单推送给护工端
 *
 * @author CareLink
 * @since 2026-02-11
 */
@Service
public class OrderPushServiceImpl implements OrderPushService {

    private static final Logger log = LoggerFactory.getLogger(OrderPushServiceImpl.class);

    /** 护工端订阅的订单推送目标前缀，完整目标为 /topic/order/{caregiverId} */
    private static final String ORDER_TOPIC_PREFIX = "/topic/order/";

    @Resource
    private SimpMessagingTemplate messagingTemplate;

    @Override
    public void pushOrderToCaregivers(Order order, List<Long> caregiverIds) {
        if (order == null || CollectionUtils.isEmpty(caregiverIds)) {
            return;
        }
        OrderPushVO vo = buildOrderPushVO(order);
        for (Long caregiverId : caregiverIds) {
            try {
                String destination = ORDER_TOPIC_PREFIX + caregiverId;  // 如 /topic/order/123
                messagingTemplate.convertAndSend(destination, vo);
                log.debug("订单推送成功 orderId={} caregiverId={}", order.getId(), caregiverId);
            } catch (Exception e) {
                log.warn("订单推送失败 orderId={} caregiverId={}", order.getId(), caregiverId, e);
            }
        }
    }

    private OrderPushVO buildOrderPushVO(Order order) {
        OrderPushVO vo = new OrderPushVO();
        vo.setOrderId(order.getId());
        vo.setOrderNo(order.getOrderNo());
        vo.setOrderType(order.getOrderType());
        vo.setStatus(2);
        vo.setSpecialRemark(order.getSpecialRemark());

        vo.setContactName(order.getContactName());
        vo.setContactPhone(order.getContactPhone());

        vo.setPackageId(order.getPackageId());
        vo.setPackageName(order.getPackageName());
        vo.setBillingMethod(order.getBillingMethod());
        vo.setUnitPrice(order.getUnitPrice());
        vo.setBuyQuantity(order.getBuyQuantity());
        vo.setTotalAmount(order.getTotalAmount());

        vo.setClientName(order.getClientName());
        vo.setClientGender(order.getClientGender());
        vo.setClientAge(order.getClientAge());
        vo.setClientHeight(order.getClientHeight());
        vo.setClientWeight(order.getClientWeight());
        vo.setIntellectStatus(order.getIntellectStatus());
        vo.setSelfCareAbility(order.getSelfCareAbility());
        vo.setMedicalHistory(order.getMedicalHistory());
        vo.setRemarks(order.getRemarks());

        vo.setAddress(order.getAddress());
        vo.setDoorNumber(order.getDoorNumber());
        vo.setDetailAddress(order.getDetailAddress());

        vo.setExpectStartTime(order.getExpectStartTime());
        return vo;
    }
}
