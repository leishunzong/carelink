package com.caregiver.carelink.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.caregiver.carelink.entity.Order;
import com.caregiver.carelink.service.OrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 订单超时与匹配重试定时任务：
 * 1. 待支付超时关闭：待支付(1) 超过 15 分钟未支付 → 已关闭(8)
 * 2. 待接单超时关闭：待接单(2) 超过 24 小时未接单 → 已关闭(8)，并模拟退款（不接入真实支付）
 * 3. 待接单重试匹配：每 10 分钟对仍在 24 小时内的待接单订单重新执行匹配派单，直到被接单或超时关闭
 */
@Component
public class OrderTimeoutTask {

    private static final Logger log = LoggerFactory.getLogger(OrderTimeoutTask.class);

    /** 待支付超时分钟数 */
    private static final int UNPAID_TIMEOUT_MINUTES = 15;
    /** 待接单超时小时数，超时后自动关闭并模拟退款 */
    private static final int UNACCEPTED_TIMEOUT_HOURS = 24;

    @Resource
    private OrderService orderService;

    /** 每分钟：关闭超时未支付订单 */
    @Scheduled(cron = "0 * * * * ?")
    public void closeUnpaidOrders() {
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(UNPAID_TIMEOUT_MINUTES);
        LambdaUpdateWrapper<Order> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(Order::getStatus, 1)
                .lt(Order::getCreateTime, threshold)
                .set(Order::getStatus, 8);
        orderService.update(wrapper);
    }

    /** 每 10 分钟：先关闭超时未接单订单（模拟退款），再对未超时的待接单订单重试匹配 */
    @Scheduled(cron = "0 */10 * * * ?")
    public void closeUnacceptedTimeoutAndRetryMatch() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime threshold = now.minusHours(UNACCEPTED_TIMEOUT_HOURS);

        // 1. 待接单超过 24 小时 → 自动关闭，模拟退款（仅更新状态与原因，不调用真实支付）
        LambdaUpdateWrapper<Order> closeWrapper = new LambdaUpdateWrapper<>();
        closeWrapper.eq(Order::getStatus, 2)
                .lt(Order::getCreateTime, threshold)
                .set(Order::getStatus, 8)
                .set(Order::getCancelTime, now)
                .set(Order::getCancelReason, "超时未接单自动关闭，已模拟退款");
        orderService.update(closeWrapper);

        // 2. 仍在 24 小时内的待接单订单 → 仅匹配订单重试派单（定向预约不重试）
        List<Order> pendingList = orderService.list(
                new LambdaQueryWrapper<Order>()
                        .eq(Order::getStatus, 2)
                        .eq(Order::getOrderType, 1)
                        .ge(Order::getCreateTime, threshold));
        for (Order order : pendingList) {
            try {
                orderService.matchAndPushOrderAsync(order.getId());
            } catch (Exception e) {
                log.warn("重试匹配派单失败 orderId={}", order.getId(), e);
            }
        }
    }
}
