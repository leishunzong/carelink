package com.caregiver.carelink.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.caregiver.carelink.common.context.CaregiverContextHolder;
import com.caregiver.carelink.common.context.UserContextHolder;
import com.caregiver.carelink.common.result.PageResult;
import com.caregiver.carelink.common.result.Result;
import com.caregiver.carelink.dto.DirectOrderCreateDTO;
import com.caregiver.carelink.dto.OrderCancelDTO;
import com.caregiver.carelink.dto.OrderPayDTO;
import com.caregiver.carelink.dto.MatchOrderCreateDTO;
import com.caregiver.carelink.dto.StartServiceDTO;
import com.caregiver.carelink.service.OrderService;
import com.caregiver.carelink.vo.OrderDetailVO;
import com.caregiver.carelink.vo.OrderListItemVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;

/**
 * 订单控制器（匹配订单 / 定向订单）
 *
 * @author CareLink
 * @since 2026-02-11
 */
@Api(tags = "订单管理")
@RestController
@RequestMapping("/order")
public class OrderController {

    @Resource
    private OrderService orderService;

    // ==================== 匹配订单（选服务包 → 下单 → 支付 → 系统匹配派单） ====================

    @ApiOperation("创建匹配订单（选服务包下单，待支付；支付后系统匹配护工派单）")
    @PostMapping("/user/match/create")
    public Result<Map<String, Object>> createMatchOrder(@Validated @RequestBody MatchOrderCreateDTO dto) {
        Long userId = UserContextHolder.getUserId();
        Long orderId = orderService.createMatchOrder(userId, dto);
        Map<String, Object> data = new HashMap<>(2);
        data.put("orderId", orderId);
        data.put("message", "匹配订单已创建，请尽快支付，支付后将为您匹配护工。");
        return Result.success(data);
    }

    @ApiOperation("支付订单（模拟支付；匹配订单派单推送，定向订单直接推送给指定护工，内部按订单类型处理）")
    @PostMapping("/user/pay")
    public Result<String> payOrder(@Validated @RequestBody OrderPayDTO dto) {
        Long userId = UserContextHolder.getUserId();
        orderService.payOrder(userId, dto.getOrderId());
        return Result.success("支付成功");
    }

    // ==================== 定向预约（指定护工 → 下单 → 支付用上面 /user/pay） ====================

    @ApiOperation("创建定向预约订单（指定护工、选服务包，待支付；护工须为接单中且已开通该服务包）")
    @PostMapping("/user/direct/create")
    public Result<Map<String, Object>> createDirectOrder(@Validated @RequestBody DirectOrderCreateDTO dto) {
        Long userId = UserContextHolder.getUserId();
        Long orderId = orderService.createDirectOrder(userId, dto);
        Map<String, Object> data = new HashMap<>(2);
        data.put("orderId", orderId);
        data.put("message", "定向预约订单已创建，请尽快支付，支付后将推送给该护工。");
        return Result.success(data);
    }

    @ApiOperation("用户取消订单（待支付、待接单可取消；待上门在预约时间过后的约定间隔内且护工未上门也可取消并退款）")
    @PostMapping("/user/cancel/{orderId}")
    public Result<Void> cancelOrder(
            @ApiParam(value = "订单ID", required = true) @PathVariable Long orderId,
            @RequestBody(required = false) OrderCancelDTO dto) {
        Long userId = UserContextHolder.getUserId();
        orderService.cancelOrder(userId, orderId, dto);
        return Result.success("已取消");
    }

    @ApiOperation("用户分页查询自己的订单列表（含匹配/定向，按状态、服务包类型筛选；orderType 由前端区分）")
    @GetMapping("/user/page")
    public Result<PageResult<OrderListItemVO>> getOrderPageForUser(
            @ApiParam("订单状态：1-待支付 2-待接单 3-待上门 4-服务中 5-待确认 6-已完成 7-已取消 8-已关闭") @RequestParam(required = false) Integer status,
            @ApiParam("服务包类型：1-居家陪护 2-医院陪护 3-周期护理 4-家政服务 5-陪诊服务 6-母婴护理") @RequestParam(required = false) Integer category,
            @ApiParam("页码") @RequestParam(defaultValue = "1") Long current,
            @ApiParam("每页数量") @RequestParam(defaultValue = "10") Long size) {
        Long userId = UserContextHolder.getUserId();
        IPage<OrderDetailVO> page = orderService.getOrderPageForUser(userId, status, category, current, size);
        IPage<OrderListItemVO> listPage = page.convert(this::toListItemVO);
        return Result.success(PageResult.of(listPage));
    }

    @ApiOperation("用户根据订单ID查询订单详情（含匹配/定向）")
    @GetMapping("/user/{orderId}")
    public Result<OrderDetailVO> getOrderDetailForUser(
            @ApiParam(value = "订单ID", required = true) @PathVariable Long orderId) {
        Long userId = UserContextHolder.getUserId();
        OrderDetailVO vo = orderService.getOrderDetailForUser(userId, orderId);
        return Result.success(vo);
    }

    @ApiOperation("用户确认订单完成（仅待确认状态可操作；匹配/定向订单通用）")
    @PostMapping("/user/complete/{orderId}")
    public Result<Void> completeOrder(
            @ApiParam(value = "订单ID", required = true) @PathVariable Long orderId) {
        Long userId = UserContextHolder.getUserId();
        orderService.completeOrder(userId, orderId);
        return Result.success("已确认完成");
    }

    // ==================== 护工端：订单 ====================

    @ApiOperation("护工分页查询自己的订单列表（含匹配/定向，按状态、服务包类型筛选；orderType 由前端区分）")
    @GetMapping("/caregiver/page")
    public Result<PageResult<OrderListItemVO>> getOrderPageForCaregiver(
            @ApiParam("订单状态：1-待支付 2-待接单 3-待上门 4-服务中 5-待确认 6-已完成 7-已取消 8-已关闭") @RequestParam(required = false) Integer status,
            @ApiParam("服务包类型：1-居家陪护 2-医院陪护 3-周期护理 4-家政服务 5-陪诊服务 6-母婴护理") @RequestParam(required = false) Integer category,
            @ApiParam("页码") @RequestParam(defaultValue = "1") Long current,
            @ApiParam("每页数量") @RequestParam(defaultValue = "10") Long size) {
        Long caregiverId = CaregiverContextHolder.getCaregiverId();
        IPage<OrderDetailVO> page = orderService.getOrderPageForCaregiver(caregiverId, status, category, current, size);
        IPage<OrderListItemVO> listPage = page.convert(this::toListItemVO);
        return Result.success(PageResult.of(listPage));
    }

    @ApiOperation("护工根据订单ID查询订单详情（仅能查已接单的订单，含匹配/定向）")
    @GetMapping("/caregiver/{orderId}")
    public Result<OrderDetailVO> getOrderDetailForCaregiver(
            @ApiParam(value = "订单ID", required = true) @PathVariable Long orderId) {
        Long caregiverId = CaregiverContextHolder.getCaregiverId();
        OrderDetailVO vo = orderService.getOrderDetailForCaregiver(caregiverId, orderId);
        return Result.success(vo);
    }

    @ApiOperation("护工抢单（仅限收到推送的订单；多护工同时抢时仅一人成功）")
    @PostMapping("/caregiver/grab/{orderId}")
    public Result<Void> grabOrder(
            @ApiParam(value = "订单ID", required = true) @PathVariable Long orderId) {
        Long caregiverId = CaregiverContextHolder.getCaregiverId();
        orderService.grabOrder(caregiverId, orderId);
        return Result.success("接单成功");
    }

    @ApiOperation("护工上门打卡/开始服务（待上门→服务中，记录实际开始时间；需上传当前定位，距服务地址500米内方可打卡）")
    @PostMapping("/caregiver/start/{orderId}")
    public Result<Void> startService(
            @ApiParam(value = "订单ID", required = true) @PathVariable Long orderId,
            @Validated @RequestBody StartServiceDTO dto) {
        Long caregiverId = CaregiverContextHolder.getCaregiverId();
        orderService.startService(caregiverId, orderId, dto.getLongitude(), dto.getLatitude());
        return Result.success("已开始服务");
    }

    @ApiOperation("护工结束服务（服务中→待确认，后续由用户验收并确认完成）")
    @PostMapping("/caregiver/finish/{orderId}")
    public Result<Void> finishService(
            @ApiParam(value = "订单ID", required = true) @PathVariable Long orderId) {
        Long caregiverId = CaregiverContextHolder.getCaregiverId();
        orderService.finishService(caregiverId, orderId);
        return Result.success("已结束服务，等待用户确认");
    }

    /**
     * 订单详情 -> 列表项字段精简映射
     */
    private OrderListItemVO toListItemVO(OrderDetailVO detail) {
        OrderListItemVO vo = new OrderListItemVO();
        vo.setId(detail.getId());
        vo.setOrderNo(detail.getOrderNo());
        vo.setOrderType(detail.getOrderType());
        vo.setStatus(detail.getStatus());
        vo.setPackageId(detail.getPackageId());
        vo.setPackageName(detail.getPackageName());
        vo.setCaregiverName(detail.getCaregiverName());
        vo.setAddress(detail.getAddress());
        vo.setDoorNumber(detail.getDoorNumber());
        vo.setDetailAddress(detail.getDetailAddress());
        vo.setBillingMethod(detail.getBillingMethod());
        vo.setUnitPrice(detail.getUnitPrice());
        vo.setBuyQuantity(detail.getBuyQuantity());
        vo.setTotalAmount(detail.getTotalAmount());
        vo.setClientName(detail.getClientName());
        vo.setClientGender(detail.getClientGender());
        vo.setClientAge(detail.getClientAge());
        vo.setSelfCareAbility(detail.getSelfCareAbility());
        vo.setMedicalHistory(detail.getMedicalHistory());
        vo.setContactName(detail.getContactName());
        vo.setContactPhone(detail.getContactPhone());
        vo.setMatchingRadius(detail.getMatchingRadius());
        vo.setReqGender(detail.getReqGender());
        vo.setReqWorkYears(detail.getReqWorkYears());
        vo.setReqNativePlace(detail.getReqNativePlace());
        vo.setSpecialRemark(detail.getSpecialRemark());
        vo.setExpectStartTime(detail.getExpectStartTime());
        vo.setCreateTime(detail.getCreateTime());
        return vo;
    }
}
