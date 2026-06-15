package com.caregiver.carelink.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.caregiver.carelink.dto.DirectOrderCreateDTO;
import com.caregiver.carelink.dto.OrderCancelDTO;
import com.caregiver.carelink.dto.MatchOrderCreateDTO;
import com.caregiver.carelink.entity.Order;
import com.caregiver.carelink.vo.AdminOrderListItemVO;
import com.caregiver.carelink.vo.OrderDetailVO;
import com.caregiver.carelink.vo.OrderListItemVO;

import java.math.BigDecimal;

/**
 * 订单服务
 *
 * @author CareLink
 * @since 2026-02-11
 */
public interface OrderService extends IService<Order> {

    /**
     * 用户创建匹配订单（选服务包下单，待支付）；未传 cityCode 时从用户表补全。支付成功后系统匹配派单。
     *
     * @param userId 当前用户ID
     * @param dto    提交参数
     * @return 订单ID
     */
    Long createMatchOrder(Long userId, MatchOrderCreateDTO dto);

    /**
     * 用户创建定向预约订单（指定护工、选服务包）；无需匹配半径与护工要求。支付后直接推送给该护工。
     *
     * @param userId 当前用户ID
     * @param dto    提交参数（含 caregiverId）
     * @return 订单ID
     */
    Long createDirectOrder(Long userId, DirectOrderCreateDTO dto);

    /**
     * 用户支付订单（模拟）：状态 1→2。匹配订单则异步匹配派单；定向订单则直接推送给指定护工。
     *
     * @param userId  当前用户ID
     * @param orderId 订单ID
     */
    void payOrder(Long userId, Long orderId);

    /**
     * 用户取消订单：待支付、待接单可取消；待上门在预约时间+间隔后且护工未上门可取消并模拟退款。
     */
    void cancelOrder(Long userId, Long orderId, OrderCancelDTO dto);

    /**
     * 用户根据订单ID查询订单详情（仅能查本人订单，含匹配/定向）
     */
    OrderDetailVO getOrderDetailForUser(Long userId, Long orderId);

    /**
     * 护工根据订单ID查询订单详情（仅能查已接单的订单，含匹配/定向）
     */
    OrderDetailVO getOrderDetailForCaregiver(Long caregiverId, Long orderId);

    /**
     * 用户确认订单完成（仅待确认状态可确认；匹配/定向订单通用）
     */
    void completeOrder(Long userId, Long orderId);

    /**
     * 用户分页查询自己的订单列表（含匹配/定向订单，按状态、服务包类型筛选，按创建时间倒序；前端根据 orderType 区分类型）
     */
    IPage<OrderDetailVO> getOrderPageForUser(Long userId, Integer status, Integer category, long current, long size);

    /**
     * 护工分页查询自己的订单列表（含匹配/定向订单，按状态、服务包类型筛选，按创建时间倒序；前端根据 orderType 区分类型）
     */
    IPage<OrderDetailVO> getOrderPageForCaregiver(Long caregiverId, Integer status, Integer category, long current, long size);

    /**
     * 异步执行：根据订单匹配可派单护工，按好评率取前 N 名，通过 WebSocket 推送订单（仅当订单状态为待接单时执行）
     */
    void matchAndPushOrderAsync(Long orderId);

    /**
     * 护工抢单：仅限收到该订单推送的护工，且订单为待接单时通过原子更新接单，并发安全
     *
     * @param caregiverId 护工ID
     * @param orderId     订单ID
     */
    void grabOrder(Long caregiverId, Long orderId);

    /**
     * 护工上门打卡/开始服务：仅接单护工可操作，待上门(3)→服务中(4)，并记录实际开始时间。
     * 打卡前校验护工当前定位与订单服务地址距离，超出阈值不允许打卡。
     *
     * @param caregiverId  护工ID
     * @param orderId      订单ID
     * @param currentLng   护工当前经度
     * @param currentLat   护工当前纬度
     */
    void startService(Long caregiverId, Long orderId, BigDecimal currentLng, BigDecimal currentLat);

    /**
     * 护工结束服务：仅接单护工可操作，服务中(4)→待确认(5)，后续由用户验收并确认完成
     *
     * @param caregiverId 护工ID
     * @param orderId     订单ID
     */
    void finishService(Long caregiverId, Long orderId);

    /**
     * 管理员分页查询订单列表，支持订单号、订单类型、订单状态、护工名、联系人姓名、城市名检索，按创建时间倒序
     * 返回管理端专用列表 VO（含用户昵称、开始时间、结束时间、上门时间）
     */
    IPage<AdminOrderListItemVO> pageOrdersForAdmin(String orderNoKeyword, Integer orderType, Integer status,
                                                  String caregiverNameKeyword, String contactNameKeyword, String cityKeyword,
                                                  Long current, Long size);

    /**
     * 总营业额：已完成订单（status=6）的 total_amount 合计（元）
     */
    BigDecimal getTotalRevenue();

    /**
     * 当日营业额：今日已完成订单的 total_amount 合计（元），按 finish_time 所在自然日
     */
    BigDecimal getTodayRevenue();
}
