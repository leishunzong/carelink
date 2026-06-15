package com.caregiver.carelink.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.caregiver.carelink.common.constant.RedisKeyConstants;
import com.caregiver.carelink.common.constant.WorkStateConstants;
import com.caregiver.carelink.common.exception.BusinessException;
import com.caregiver.carelink.common.result.ResultCode;
import com.caregiver.carelink.dto.DirectOrderCreateDTO;
import com.caregiver.carelink.dto.OrderCancelDTO;
import com.caregiver.carelink.dto.MatchOrderCreateDTO;
import com.caregiver.carelink.entity.Caregiver;
import com.caregiver.carelink.entity.CaregiverStats;
import com.caregiver.carelink.entity.CaregiverServiceConfig;
import com.caregiver.carelink.entity.Order;
import com.caregiver.carelink.entity.OrderPushRecord;
import com.caregiver.carelink.entity.ServicePackage;
import com.caregiver.carelink.mapper.CaregiverStatsMapper;
import com.caregiver.carelink.mapper.OrderMapper;
import com.caregiver.carelink.mapper.OrderPushRecordMapper;
import com.caregiver.carelink.entity.User;
import com.caregiver.carelink.service.CaregiverService;
import com.caregiver.carelink.service.CaregiverServiceConfigService;
import com.caregiver.carelink.service.OrderPushService;
import com.caregiver.carelink.service.OrderService;
import com.caregiver.carelink.service.ServicePackageService;
import com.caregiver.carelink.service.UserService;
import com.caregiver.carelink.utils.RedisUtils;
import com.caregiver.carelink.vo.AdminOrderListItemVO;
import com.caregiver.carelink.vo.OrderDetailVO;
import com.caregiver.carelink.vo.OrderListItemVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

/**
 * 订单服务实现：创建订单 + 异步匹配派单 + WebSocket 推送
 *
 * @author CareLink
 * @since 2026-02-11
 */
@Service
public class OrderServiceImpl extends ServiceImpl<OrderMapper, Order> implements OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderServiceImpl.class);
    private static final int PUSH_TOP_N = 10;
    /** 上门打卡允许的最大距离（米），超出则不允许打卡 */
    private static final double MAX_START_SERVICE_DISTANCE_METERS = 500;
    /** 待上门的状态可取消的间隔（分钟）：当前时间须在预约上门的时间之后超过该间隔且护工未打卡，用户方可取消并退款 */
    private static final int CANCEL_PENDING_VISIT_BUFFER_MINUTES = 30;

    @Resource
    private ServicePackageService servicePackageService;

    @Resource
    private CaregiverService caregiverService;

    @Resource
    private CaregiverStatsMapper caregiverStatsMapper;

    @Resource
    private RedisUtils redisUtils;

    @Resource
    private OrderPushService orderPushService;

    @Resource
    private OrderPushRecordMapper orderPushRecordMapper;

    @Resource
    private UserService userService;

    @Resource
    private CaregiverServiceConfigService caregiverServiceConfigService;

    @Override
    public Long createMatchOrder(Long userId, MatchOrderCreateDTO dto) {
        log.info("创建匹配订单 userId={}, packageId={}", userId, dto.getPackageId());
        ServicePackage pkg = servicePackageService.getById(dto.getPackageId());
        if (pkg == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "服务包不存在");
        }
        Order order = new Order();
        BeanUtil.copyProperties(dto, order);
        order.setId(null);
        order.setOrderNo(generateOrderNo());
        order.setOrderType(1);
        order.setStatus(1);
        order.setUserId(userId);
        order.setCaregiverId(null);
        order.setDetailAddress(buildDetailAddress(dto.getAddress(), dto.getDoorNumber()));
        if (dto.getMatchingRadius() == null) {
            order.setMatchingRadius(10);
        }
        if (dto.getReqGender() == null) {
            order.setReqGender(0);
        }
        if (dto.getReqWorkYears() == null) {
            order.setReqWorkYears(0);
        }
        if (!StringUtils.hasText(order.getCityCode())) {
            User user = userService.getById(userId);
            if (user != null && StringUtils.hasText(user.getCityCode())) {
                order.setCityCode(user.getCityCode());
            }
        }
        save(order);
        log.info("匹配订单创建成功 orderId={}, orderNo={}, userId={}, packageId={}, totalAmount={}",
                order.getId(), order.getOrderNo(), userId, dto.getPackageId(), order.getTotalAmount());
        return order.getId();
    }

    @Override
    public Long createDirectOrder(Long userId, DirectOrderCreateDTO dto) {
        log.info("创建定向预约订单 userId={}, caregiverId={}, packageId={}", userId, dto.getCaregiverId(), dto.getPackageId());
        ServicePackage pkg = servicePackageService.getById(dto.getPackageId());
        if (pkg == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "服务包不存在");
        }
        Caregiver caregiver = caregiverService.getById(dto.getCaregiverId());
        if (caregiver == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "护工不存在");
        }
        if (caregiver.getWorkState() == null || !caregiver.getWorkState().equals(WorkStateConstants.WORK_STATE_AVAILABLE)) {
            throw new BusinessException("该护工当前不是接单中状态，无法预约");
        }
        long hasPackage = caregiverServiceConfigService.count(
                new LambdaQueryWrapper<CaregiverServiceConfig>()
                        .eq(CaregiverServiceConfig::getCaregiverId, dto.getCaregiverId())
                        .eq(CaregiverServiceConfig::getPackageId, dto.getPackageId()));
        if (hasPackage == 0) {
            throw new BusinessException("该护工未开通此服务包，无法预约");
        }
        Order order = new Order();
        BeanUtil.copyProperties(dto, order);
        order.setId(null);
        order.setOrderNo(generateOrderNo());
        order.setOrderType(2);
        order.setStatus(1);
        order.setUserId(userId);
        order.setCaregiverId(dto.getCaregiverId());
        order.setDetailAddress(buildDetailAddress(dto.getAddress(), dto.getDoorNumber()));
        order.setMatchingRadius(0);
        order.setReqGender(0);
        order.setReqWorkYears(0);
        order.setReqNativePlace(null);
        if (!StringUtils.hasText(order.getCityCode())) {
            User user = userService.getById(userId);
            if (user != null && StringUtils.hasText(user.getCityCode())) {
                order.setCityCode(user.getCityCode());
            }
        }
        save(order);
        log.info("定向预约订单创建成功 orderId={}, orderNo={}, userId={}, caregiverId={}, packageId={}, totalAmount={}",
                order.getId(), order.getOrderNo(), userId, dto.getCaregiverId(), dto.getPackageId(), order.getTotalAmount());
        return order.getId();
    }

    @Override
    public void payOrder(Long userId, Long orderId) {
        log.info("订单支付 userId={}, orderId={}", userId, orderId);
        Order order = getById(orderId);
        if (order == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "订单不存在");
        }
        if (!order.getUserId().equals(userId)) {
            throw new BusinessException("无权操作该订单");
        }
        if (order.getStatus() != null && order.getStatus() != 1) {
            throw new BusinessException("订单状态不允许支付");
        }
        order.setStatus(2);
        updateById(order);
        log.info("订单支付成功 orderId={}, orderNo={}, userId={}, orderType={}, totalAmount={}",
                orderId, order.getOrderNo(), userId, order.getOrderType(), order.getTotalAmount());
        if (order.getOrderType() != null && order.getOrderType() == 2) {
            pushDirectOrderToCaregiver(orderId);
        } else {
            matchAndPushOrderAsync(orderId);
        }
    }

    /**
     * 定向订单支付后：直接推送给订单指定的护工（写入推送记录并 WebSocket 推送）
     */
    private void pushDirectOrderToCaregiver(Long orderId) {
        log.info("[定向推送] 开始处理 orderId={}", orderId);
        Order order = getById(orderId);
        if (order == null || order.getStatus() == null || order.getStatus() != 2) {
            log.warn("[定向推送] 订单状态不满足条件 orderId={}, status={}", orderId, order != null ? order.getStatus() : null);
            return;
        }
        Long caregiverId = order.getCaregiverId();
        if (caregiverId == null) {
            log.warn("[定向推送] 订单未指定护工 orderId={}", orderId);
            return;
        }
        log.info("[定向推送] 推送给指定护工 orderId={}, caregiverId={}", orderId, caregiverId);
        OrderPushRecord record = new OrderPushRecord();
        record.setOrderId(orderId);
        record.setCaregiverId(caregiverId);
        orderPushRecordMapper.insert(record);
        orderPushService.pushOrderToCaregivers(order, Collections.singletonList(caregiverId));
        log.info("[定向推送] 推送完成 orderId={}, caregiverId={}", orderId, caregiverId);
    }

    @Async("taskExecutor")
    @Override
    public void matchAndPushOrderAsync(Long orderId) {
        log.info("[匹配推送] 开始处理 orderId={}", orderId);
        Order order = getById(orderId);
        if (order == null || order.getStatus() == null || order.getStatus() != 2) {
            log.warn("[匹配推送] 订单状态不满足条件 orderId={}, status={}", orderId, order != null ? order.getStatus() : null);
            return;
        }
        String cityCode = order.getCityCode();
        if (!StringUtils.hasText(cityCode)) {
            log.warn("[匹配推送] 订单缺少城市编码 orderId={}", orderId);
            return;
        }
        double lng = order.getLongitude().doubleValue();
        double lat = order.getLatitude().doubleValue();
        double radiusKm = order.getMatchingRadius() != null ? order.getMatchingRadius() : 10;
        Long packageId = order.getPackageId();
        log.info("[匹配推送] 订单信息 orderId={}, cityCode={}, 位置({},{})，匹配半径={}km, packageId={}",
                orderId, cityCode, lng, lat, radiusKm, packageId);

        String geoKey = RedisKeyConstants.getCaregiverLocationGeoKey(cityCode);
        List<org.springframework.data.geo.GeoResult<RedisGeoCommands.GeoLocation<Object>>> geoResults =
                redisUtils.geoSearchNearby(geoKey, lng, lat, radiusKm, 200L);
        Set<Long> nearbyCaregiverIds = geoResults.stream()
                .map(r -> {
                    Object name = r.getContent().getName();
                    if (name == null) return null;
                    try {
                        return Long.parseLong(name.toString());
                    } catch (NumberFormatException e) {
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        log.info("[匹配推送] 位置匹配完成 orderId={}, 附近护工数={}", orderId, nearbyCaregiverIds.size());

        String packageKey = RedisKeyConstants.getPackageCaregiversKey(packageId);
        Set<Object> packageCaregiverSet = redisUtils.sGet(packageKey);
        if (packageCaregiverSet == null) {
            packageCaregiverSet = Collections.emptySet();
        }
        Set<Long> packageCaregiverIds = packageCaregiverSet.stream()
                .map(o -> {
                    try {
                        return Long.parseLong(o.toString());
                    } catch (Exception e) {
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        log.info("[匹配推送] 服务包护工数 orderId={}, packageId={}, 护工数={}", orderId, packageId, packageCaregiverIds.size());

        nearbyCaregiverIds.retainAll(packageCaregiverIds);
        log.info("[匹配推送] 取交集后 orderId={}, 候选护工数={}", orderId, nearbyCaregiverIds.size());
        if (nearbyCaregiverIds.isEmpty()) {
            log.warn("[匹配推送] 无候选护工 orderId={}", orderId);
            return;
        }

        List<Caregiver> caregivers = caregiverService.listByIds(nearbyCaregiverIds);
        Integer reqGender = order.getReqGender() != null ? order.getReqGender() : 0;
        Integer reqWorkYears = order.getReqWorkYears() != null ? order.getReqWorkYears() : 0;
        String reqNativePlace = order.getReqNativePlace();
        log.info("[匹配推送] 筛选条件 orderId={}, 要求性别={}, 工龄>={}, 籍贯={}", orderId, reqGender, reqWorkYears, reqNativePlace);

        List<Caregiver> filtered = caregivers.stream()
                .filter(c -> WorkStateConstants.WORK_STATE_AVAILABLE.equals(c.getWorkState()))
                .filter(c -> reqGender == 0 || (c.getGender() != null && c.getGender().equals(reqGender)))
                .filter(c -> c.getWorkYears() != null && c.getWorkYears() >= reqWorkYears)
                .filter(c -> !StringUtils.hasText(reqNativePlace) || (c.getNativePlace() != null && c.getNativePlace().contains(reqNativePlace)))
                .collect(Collectors.toList());

        log.info("[匹配推送] 条件筛选后 orderId={}, 符合条件护工数={}", orderId, filtered.size());
        if (filtered.isEmpty()) {
            log.warn("[匹配推送] 无符合条件护工 orderId={}", orderId);
            return;
        }

        List<Long> caregiverIds = filtered.stream().map(Caregiver::getId).collect(Collectors.toList());
        List<CaregiverStats> statsList = caregiverStatsMapper.selectList(
                new LambdaQueryWrapper<CaregiverStats>().in(CaregiverStats::getCaregiverId, caregiverIds));
        Map<Long, BigDecimal> rateMap = statsList.stream()
                .collect(Collectors.toMap(CaregiverStats::getCaregiverId, s -> s.getGoodReviewRate() != null ? s.getGoodReviewRate() : BigDecimal.ZERO));

        // 排除已推送过的护工，重试时只向"未推送过"的候选人里按好评率取前 N 推送
        Set<Long> alreadyPushed = orderPushRecordMapper.selectList(
                        new LambdaQueryWrapper<OrderPushRecord>().eq(OrderPushRecord::getOrderId, orderId))
                .stream()
                .map(OrderPushRecord::getCaregiverId)
                .collect(Collectors.toSet());
        if (!alreadyPushed.isEmpty()) {
            log.info("[匹配推送] 已推送过的护工 orderId={}, 已推送={}", orderId, alreadyPushed);
        }
        List<Long> toPush = caregiverIds.stream()
                .filter(id -> !alreadyPushed.contains(id))
                .sorted((a, b) -> {
                    BigDecimal ra = rateMap.getOrDefault(a, BigDecimal.ZERO);
                    BigDecimal rb = rateMap.getOrDefault(b, BigDecimal.ZERO);
                    return rb.compareTo(ra);
                })
                .limit(PUSH_TOP_N)
                .collect(Collectors.toList());
        log.info("[匹配推送] 待推送护工列表 orderId={}, 待推送数={}, 护工ID列表={}", orderId, toPush.size(), toPush);
        if (toPush.isEmpty()) {
            log.warn("[匹配推送] 无新护工可推送 orderId={}", orderId);
            return;
        }

        for (Long caregiverId : toPush) {
            OrderPushRecord record = new OrderPushRecord();
            record.setOrderId(orderId);
            record.setCaregiverId(caregiverId);
            orderPushRecordMapper.insert(record);
        }
        orderPushService.pushOrderToCaregivers(order, toPush);
        log.info("[匹配推送] 推送完成 orderId={}, 推送给护工={}", orderId, toPush);
    }

    @Override
    public void grabOrder(Long caregiverId, Long orderId) {
        log.info("护工抢单 caregiverId={}, orderId={}", caregiverId, orderId);
        Order order = getById(orderId);
        if (order == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "订单不存在");
        }
        Integer status = order.getStatus();
        if (status == null) {
            throw new BusinessException("订单状态异常，无法接单");
        }
        if (!status.equals(2)) {
            if (status >= 3 && status <= 6) {
                throw new BusinessException("订单已被其他护工接单");
            }
            throw new BusinessException("订单状态不允许接单，仅待接单订单可抢");
        }
        long pushed = orderPushRecordMapper.selectCount(
                new LambdaQueryWrapper<OrderPushRecord>()
                        .eq(OrderPushRecord::getOrderId, orderId)
                        .eq(OrderPushRecord::getCaregiverId, caregiverId));
        if (pushed == 0) {
            throw new BusinessException("您未收到该订单推送，无法接单");
        }
        // 乐观锁：仅当 status=2 时更新，多护工同时抢单时只有一人更新成功
        LambdaUpdateWrapper<Order> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(Order::getId, orderId)
                .eq(Order::getStatus, 2)
                .set(Order::getStatus, 3)
                .set(Order::getCaregiverId, caregiverId);
        boolean updated = update(wrapper);
        if (!updated) {
            throw new BusinessException("订单已被其他护工接单");
        }
        log.info("护工抢单成功 caregiverId={}, orderId={}, orderNo={}", caregiverId, orderId, order.getOrderNo());

        // 护工工作状态改为服务中，并从 Redis GEO 移除（不再参与新订单匹配）
        Caregiver caregiver = caregiverService.getById(caregiverId);
        if (caregiver != null) {
            caregiver.setWorkState(WorkStateConstants.WORK_STATE_BUSY);
            caregiverService.updateById(caregiver);
            if (StringUtils.hasText(caregiver.getCityCode())) {
                String geoKey = RedisKeyConstants.getCaregiverLocationGeoKey(caregiver.getCityCode());
                String member = RedisKeyConstants.getCaregiverGeoMember(caregiverId);
                redisUtils.geoRemove(geoKey, member);
            }
            log.info("护工状态变更为服务中并移出匹配池 caregiverId={}, orderId={}", caregiverId, orderId);
        }
    }

    @Override
    public void startService(Long caregiverId, Long orderId, BigDecimal currentLng, BigDecimal currentLat) {
        log.info("护工上门打卡 caregiverId={}, orderId={}", caregiverId, orderId);
        Order order = getById(orderId);
        if (order == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "订单不存在");
        }
        if (order.getCaregiverId() == null || !order.getCaregiverId().equals(caregiverId)) {
            throw new BusinessException("无权操作该订单，仅接单护工可上门打卡");
        }
        Integer status = order.getStatus();
        if (status == null) {
            throw new BusinessException("订单状态异常");
        }
        if (status.equals(4) || status.equals(5) || status.equals(6)) {
            return;
        }
        if (!status.equals(3)) {
            throw new BusinessException("仅待上门状态可进行上门打卡");
        }
        if (currentLng == null || currentLat == null) {
            throw new BusinessException("请上传当前定位以完成打卡");
        }
        if (order.getLongitude() == null || order.getLatitude() == null) {
            throw new BusinessException("订单服务地址缺少坐标，无法校验打卡位置");
        }
        double distanceMeters = haversineDistanceMeters(
                order.getLatitude().doubleValue(), order.getLongitude().doubleValue(),
                currentLat.doubleValue(), currentLng.doubleValue());
        if (distanceMeters > MAX_START_SERVICE_DISTANCE_METERS) {
            throw new BusinessException("您当前距离服务地址过远，请到达后再打卡（距服务地址约 " + (int) distanceMeters + " 米）");
        }
        order.setStatus(4);
        order.setRealStartTime(LocalDateTime.now());
        updateById(order);
        log.info("护工上门打卡成功 caregiverId={}, orderId={}, orderNo={}, 距离={}米",
                caregiverId, orderId, order.getOrderNo(), (int) distanceMeters);

        // 护工工作状态改为服务中，并从 Redis GEO 移除（不再参与匹配）
        Caregiver caregiver = caregiverService.getById(caregiverId);
        if (caregiver != null) {
            caregiver.setWorkState(WorkStateConstants.WORK_STATE_BUSY);
            caregiverService.updateById(caregiver);
            if (StringUtils.hasText(caregiver.getCityCode())) {
                String geoKey = RedisKeyConstants.getCaregiverLocationGeoKey(caregiver.getCityCode());
                String member = RedisKeyConstants.getCaregiverGeoMember(caregiverId);
                redisUtils.geoRemove(geoKey, member);
            }
        }
    }

    @Override
    public void finishService(Long caregiverId, Long orderId) {
        log.info("护工结束服务 caregiverId={}, orderId={}", caregiverId, orderId);
        Order order = getById(orderId);
        if (order == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "订单不存在");
        }
        if (order.getCaregiverId() == null || !order.getCaregiverId().equals(caregiverId)) {
            throw new BusinessException("无权操作该订单，仅接单护工可结束服务");
        }
        Integer status = order.getStatus();
        if (status == null) {
            throw new BusinessException("订单状态异常");
        }
        if (status.equals(5) || status.equals(6)) {
            return;
        }
        if (!status.equals(4)) {
            throw new BusinessException("仅服务中状态可结束服务");
        }
        order.setStatus(5);
        updateById(order);
        log.info("护工结束服务成功 caregiverId={}, orderId={}, orderNo={}", caregiverId, orderId, order.getOrderNo());
    }

    /**
     * 计算两点经纬度的球面距离（米），Haversine 公式
     */
    private static double haversineDistanceMeters(double lat1Deg, double lon1Deg, double lat2Deg, double lon2Deg) {
        double R = 6371000;
        double phi1 = Math.toRadians(lat1Deg);
        double phi2 = Math.toRadians(lat2Deg);
        double dPhi = Math.toRadians(lat2Deg - lat1Deg);
        double dLambda = Math.toRadians(lon2Deg - lon1Deg);
        double a = Math.sin(dPhi / 2) * Math.sin(dPhi / 2)
                + Math.cos(phi1) * Math.cos(phi2) * Math.sin(dLambda / 2) * Math.sin(dLambda / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    @Override
    public void cancelOrder(Long userId, Long orderId, OrderCancelDTO dto) {
        log.info("用户取消订单 userId={}, orderId={}", userId, orderId);
        Order order = getById(orderId);
        if (order == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "订单不存在");
        }
        if (!order.getUserId().equals(userId)) {
            throw new BusinessException("无权操作该订单");
        }
        Integer status = order.getStatus();
        if (status == null) {
            throw new BusinessException("当前状态不允许取消");
        }
        if (status == 1 || status == 2) {
            // 待支付、待接单：直接可取消
        } else if (status == 3) {
            // 待上门：仅当当前时间在预约上门时间之后且超过约定间隔，且护工未上门（未打卡）时允许取消并退款
            LocalDateTime expectStart = order.getExpectStartTime();
            if (expectStart == null) {
                throw new BusinessException("当前状态不允许取消，待上门订单缺少预约上门时间");
            }
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime cancelAllowedAfter = expectStart.plusMinutes(CANCEL_PENDING_VISIT_BUFFER_MINUTES);
            if (now.isBefore(cancelAllowedAfter)) {
                throw new BusinessException("待上门订单须在预约上门时间过后" + CANCEL_PENDING_VISIT_BUFFER_MINUTES + "分钟且护工未上门时方可取消");
            }
            if (order.getRealStartTime() != null) {
                throw new BusinessException("护工已上门开始服务，无法取消");
            }
            // 满足条件：取消并模拟退款（仅更新状态与原因，不调用真实支付）
            log.info("待上门订单取消并模拟退款 orderId={}, userId={}, totalAmount={}", orderId, userId, order.getTotalAmount());
        } else {
            throw new BusinessException("当前状态不允许取消，仅待支付、待接单或满足条件的待上门订单可取消");
        }
        order.setStatus(7);
        order.setCancelTime(LocalDateTime.now());
        String cancelReason = (dto != null && StringUtils.hasText(dto.getCancelReason())) ? dto.getCancelReason() : "用户取消";
        order.setCancelReason(cancelReason);
        updateById(order);
        log.info("订单取消成功 orderId={}, orderNo={}, userId={}, 原状态={}, 取消原因={}",
                orderId, order.getOrderNo(), userId, status, cancelReason);

        // 待上门的状态下用户取消：护工已接单但未上门，视为护工爽约，绩效统计表取消次数 +1
        if (status == 3 && order.getCaregiverId() != null) {
            caregiverStatsMapper.incrementCancelCount(order.getCaregiverId());

            // 恢复护工工作状态为"接单中"，并重新加入 Redis GEO 匹配池
            Caregiver caregiver = caregiverService.getById(order.getCaregiverId());
            if (caregiver != null) {
                caregiver.setWorkState(WorkStateConstants.WORK_STATE_AVAILABLE);
                caregiverService.updateById(caregiver);
                if (StringUtils.hasText(caregiver.getCityCode()) && caregiver.getLongitude() != null && caregiver.getLatitude() != null) {
                    String geoKey = RedisKeyConstants.getCaregiverLocationGeoKey(caregiver.getCityCode());
                    String member = RedisKeyConstants.getCaregiverGeoMember(caregiver.getId());
                    redisUtils.geoAdd(geoKey, caregiver.getLongitude().doubleValue(), caregiver.getLatitude().doubleValue(), member);
                }
                log.info("待上门订单取消，护工状态恢复为接单中并重新加入匹配池 caregiverId={}, orderId={}", caregiver.getId(), orderId);
            }
        }
    }

    @Override
    public OrderDetailVO getOrderDetailForUser(Long userId, Long orderId) {
        Order order = getById(orderId);
        if (order == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "订单不存在");
        }
        if (!order.getUserId().equals(userId)) {
            throw new BusinessException("无权查看该订单");
        }
        return orderToDetailVO(order);
    }

    @Override
    public OrderDetailVO getOrderDetailForCaregiver(Long caregiverId, Long orderId) {
        Order order = getById(orderId);
        if (order == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "订单不存在");
        }
        if (order.getCaregiverId() == null || !order.getCaregiverId().equals(caregiverId)) {
            throw new BusinessException("无权查看该订单");
        }
        return orderToDetailVO(order);
    }

    @Override
    public void completeOrder(Long userId, Long orderId) {
        log.info("用户确认订单完成 userId={}, orderId={}", userId, orderId);
        Order order = getById(orderId);
        if (order == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "订单不存在");
        }
        if (!order.getUserId().equals(userId)) {
            throw new BusinessException("无权操作该订单");
        }
        if (order.getStatus() == null || !order.getStatus().equals(5)) {
            throw new BusinessException("当前状态不允许确认完成，仅待确认状态可操作");
        }
        order.setStatus(6);
        order.setFinishTime(LocalDateTime.now());
        updateById(order);
        log.info("订单完成 orderId={}, orderNo={}, userId={}, caregiverId={}, totalAmount={}",
                orderId, order.getOrderNo(), userId, order.getCaregiverId(), order.getTotalAmount());

        // 护工统计表完单量 +1（数据库原子更新，并发安全无需加锁）
        if (order.getCaregiverId() != null) {
            caregiverStatsMapper.incrementOrderCount(order.getCaregiverId());
        }
        // 服务包销量 +1
        if (order.getPackageId() != null) {
            ServicePackage pkg = servicePackageService.getById(order.getPackageId());
            if (pkg != null) {
                pkg.setSales(pkg.getSales() == null ? 1 : pkg.getSales() + 1);
                servicePackageService.updateById(pkg);
            }
        }
        // 护工工作状态改回接单中，并重新加入 Redis GEO 参与匹配
        if (order.getCaregiverId() != null) {
            Caregiver caregiver = caregiverService.getById(order.getCaregiverId());
            if (caregiver != null) {
                caregiver.setWorkState(WorkStateConstants.WORK_STATE_AVAILABLE);
                caregiverService.updateById(caregiver);
                if (StringUtils.hasText(caregiver.getCityCode()) && caregiver.getLongitude() != null && caregiver.getLatitude() != null) {
                    String geoKey = RedisKeyConstants.getCaregiverLocationGeoKey(caregiver.getCityCode());
                    String member = RedisKeyConstants.getCaregiverGeoMember(caregiver.getId());
                    redisUtils.geoAdd(geoKey, caregiver.getLongitude().doubleValue(), caregiver.getLatitude().doubleValue(), member);
                }
            }
        }
    }

    @Override
    public IPage<OrderDetailVO> getOrderPageForUser(Long userId, Integer status, Integer category, long current, long size) {
        return buildOrderPage(userId, null, status, category, current, size);
    }

    @Override
    public IPage<OrderDetailVO> getOrderPageForCaregiver(Long caregiverId, Integer status, Integer category, long current, long size) {
        return buildOrderPage(null, caregiverId, status, category, current, size);
    }

    private IPage<OrderDetailVO> buildOrderPage(Long userId, Long caregiverId, Integer status, Integer category, long current, long size) {
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(userId != null, Order::getUserId, userId)
                .eq(caregiverId != null, Order::getCaregiverId, caregiverId)
                .eq(status != null, Order::getStatus, status);
        if (category != null) {
            List<Long> packageIds = servicePackageService.list(
                    new LambdaQueryWrapper<ServicePackage>().eq(ServicePackage::getCategory, category))
                    .stream().map(ServicePackage::getId).collect(Collectors.toList());
            wrapper.in(!CollectionUtils.isEmpty(packageIds), Order::getPackageId, packageIds);
            if (CollectionUtils.isEmpty(packageIds)) {
                Page<OrderDetailVO> emptyPage = new Page<>(current, size, 0);
                emptyPage.setRecords(Collections.<OrderDetailVO>emptyList());
                return emptyPage;
            }
        }
        wrapper.orderByDesc(Order::getCreateTime);
        Page<Order> page = page(new Page<>(current, size), wrapper);
        List<OrderDetailVO> voList = page.getRecords().stream().map(this::orderToDetailVO).collect(Collectors.toList());
        Page<OrderDetailVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        voPage.setRecords(voList);
        return voPage;
    }

    private OrderDetailVO orderToDetailVO(Order order) {
        OrderDetailVO vo = new OrderDetailVO();
        BeanUtil.copyProperties(order, vo);
        if (order.getCaregiverId() != null) {
            Caregiver c = caregiverService.getById(order.getCaregiverId());
            if (c != null) {
                vo.setCaregiverName(c.getRealName());
                vo.setCaregiverPhone(c.getPhone());
                vo.setCaregiverAvatar(c.getAvatar());
            }
        }
        return vo;
    }

    private OrderListItemVO detailToListItemVO(OrderDetailVO detail) {
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

    private AdminOrderListItemVO orderToAdminListItemVO(Order order) {
        AdminOrderListItemVO vo = new AdminOrderListItemVO();
        vo.setId(order.getId());
        vo.setOrderNo(order.getOrderNo());
        vo.setOrderType(order.getOrderType());
        vo.setStatus(order.getStatus());
        vo.setPackageId(order.getPackageId());
        vo.setPackageName(order.getPackageName());
        vo.setAddress(order.getAddress());
        vo.setDoorNumber(order.getDoorNumber());
        vo.setDetailAddress(order.getDetailAddress());
        vo.setBillingMethod(order.getBillingMethod());
        vo.setBuyQuantity(order.getBuyQuantity());
        vo.setUnitPrice(order.getUnitPrice());
        vo.setTotalAmount(order.getTotalAmount());
        vo.setExpectStartTime(order.getExpectStartTime());
        vo.setRealStartTime(order.getRealStartTime());
        vo.setFinishTime(order.getFinishTime());
        vo.setCreateTime(order.getCreateTime());
        if (order.getUserId() != null) {
            User user = userService.getById(order.getUserId());
            if (user != null) {
                vo.setNickname(user.getNickname());
            }
        }
        if (order.getCaregiverId() != null) {
            Caregiver c = caregiverService.getById(order.getCaregiverId());
            if (c != null) {
                vo.setCaregiverName(c.getRealName());
            }
        }
        return vo;
    }

    @Override
    public IPage<AdminOrderListItemVO> pageOrdersForAdmin(String orderNoKeyword, Integer orderType, Integer status,
                                                          String caregiverNameKeyword, String contactNameKeyword, String cityKeyword,
                                                          Long current, Long size) {
        List<Long> caregiverIds = null;
        if (StringUtils.hasText(caregiverNameKeyword)) {
            List<Caregiver> caregivers = caregiverService.list(
                    new LambdaQueryWrapper<Caregiver>().like(Caregiver::getRealName, caregiverNameKeyword).select(Caregiver::getId));
            caregiverIds = caregivers.stream().map(Caregiver::getId).collect(Collectors.toList());
            if (caregiverIds.isEmpty()) {
                Page<AdminOrderListItemVO> emptyPage = new Page<>(current, size, 0L);
                emptyPage.setRecords(Collections.emptyList());
                return emptyPage;
            }
        }
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StringUtils.hasText(orderNoKeyword), Order::getOrderNo, orderNoKeyword)
                .eq(orderType != null, Order::getOrderType, orderType)
                .eq(status != null, Order::getStatus, status)
                .like(StringUtils.hasText(contactNameKeyword), Order::getContactName, contactNameKeyword)
                .like(StringUtils.hasText(cityKeyword), Order::getAddress, cityKeyword)
                .in(caregiverIds != null && !caregiverIds.isEmpty(), Order::getCaregiverId, caregiverIds)
                .orderByDesc(Order::getCreateTime);
        Page<Order> page = page(new Page<>(current, size), wrapper);
        List<AdminOrderListItemVO> voList = page.getRecords().stream()
                .map(this::orderToAdminListItemVO)
                .collect(Collectors.toList());
        Page<AdminOrderListItemVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        voPage.setRecords(voList);
        return voPage;
    }

    @Override
    public BigDecimal getTotalRevenue() {
        BigDecimal sum = baseMapper.sumRevenueCompleted();
        return sum != null ? sum : BigDecimal.ZERO;
    }

    @Override
    public BigDecimal getTodayRevenue() {
        BigDecimal sum = baseMapper.sumRevenueToday();
        return sum != null ? sum : BigDecimal.ZERO;
    }

    private String generateOrderNo() {
        return "ORD" + System.currentTimeMillis() + String.format("%04d", ThreadLocalRandom.current().nextInt(10000));
    }

    private String buildDetailAddress(String address, String doorNumber) {
        if (!StringUtils.hasText(address)) return "";
        return address + (StringUtils.hasText(doorNumber) ? " " + doorNumber.trim() : "");
    }
}
