package com.caregiver.carelink.tool;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.caregiver.carelink.common.context.UserContextHolder;
import com.caregiver.carelink.common.exception.BusinessException;
import com.caregiver.carelink.dto.DirectOrderCreateDTO;
import com.caregiver.carelink.dto.MatchOrderCreateDTO;
import com.caregiver.carelink.entity.Caregiver;
import com.caregiver.carelink.entity.CaregiverServiceConfig;
import com.caregiver.carelink.entity.CaregiverStats;
import com.caregiver.carelink.entity.ServiceAddress;
import com.caregiver.carelink.entity.ServiceSubject;
import com.caregiver.carelink.mapper.CaregiverMapper;
import com.caregiver.carelink.mapper.CaregiverStatsMapper;
import com.caregiver.carelink.service.*;
import com.caregiver.carelink.utils.RedisUtils;
import com.caregiver.carelink.vo.*;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.agent.tool.ToolMemoryId;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * AI 可调用的业务工具集（精简版）
 * <p>
 * 仅保留两大类工具：
 * 1. AI 搜索类：搜索护工、附近护工、服务包列表、服务包详情、护工评价摘要、护理方案推荐
 * 2. AI 下单类：查询用户地址/服务对象、创建匹配订单
 *
 * @author CareLink
 * @since 2026-03-28
 */
@Component
public class CareBusinessTools {

    private static final Logger log = LoggerFactory.getLogger(CareBusinessTools.class);
    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    /**
     * conversationId -> userId 映射，解决异步线程中 ThreadLocal 丢失的问题
     */
    private static final Map<String, Long> CONVERSATION_USER_MAP = new ConcurrentHashMap<>();

    /**
     * conversationId -> 用户实时位置信息（前端小程序传入的经纬度和城市编码）
     */
    private static final Map<String, LocationInfo> CONVERSATION_LOCATION_MAP = new ConcurrentHashMap<>();

    /**
     * 用户实时位置信息
     */
    @Data
    public static class LocationInfo {
        private BigDecimal longitude;
        private BigDecimal latitude;
        private String cityCode;

        public LocationInfo(BigDecimal longitude, BigDecimal latitude, String cityCode) {
            this.longitude = longitude;
            this.latitude = latitude;
            this.cityCode = cityCode;
        }

        public boolean isValid() {
            return longitude != null && latitude != null && StringUtils.hasText(cityCode);
        }
    }

    public static void bindUser(String conversationId, Long userId) {
        if (conversationId != null && userId != null) {
            CONVERSATION_USER_MAP.put(conversationId, userId);
        }
    }

    /**
     * 绑定前端传来的用户实时位置信息
     */
    public static void bindLocation(String conversationId, BigDecimal longitude, BigDecimal latitude, String cityCode) {
        if (conversationId != null && longitude != null && latitude != null) {
            CONVERSATION_LOCATION_MAP.put(conversationId, new LocationInfo(longitude, latitude, cityCode));
        }
    }

    public static void unbindUser(String conversationId) {
        if (conversationId != null) {
            CONVERSATION_USER_MAP.remove(conversationId);
            CONVERSATION_LOCATION_MAP.remove(conversationId);
        }
    }

    private Long resolveUserId(String conversationId) {
        Long userId = null;
        if (conversationId != null) {
            userId = CONVERSATION_USER_MAP.get(conversationId);
        }
        if (userId == null) {
            userId = UserContextHolder.getUserId();
        }
        return userId;
    }

    @Resource
    private CaregiverMapper caregiverMapper;

    @Resource
    private CaregiverStatsMapper caregiverStatsMapper;

    @Resource
    private CaregiverService caregiverService;

    @Resource
    private CaregiverSkillService caregiverSkillService;

    @Resource
    private RedisUtils redisUtils;

    @Resource
    private ServicePackageService servicePackageService;

    @Resource
    private ServiceAddressService serviceAddressService;

    @Resource
    private ServiceSubjectService serviceSubjectService;

    @Resource
    private CaregiverServiceConfigService caregiverServiceConfigService;

    @Resource
    private OrderService orderService;

    @Resource
    private UserService userService;

    @Resource
    private ReviewService reviewService;

    @Resource
    private AiSummaryService aiSummaryService;

    // ====================== 搜索类工具 ======================

    @Tool("【AI搜索】搜索护工。根据关键词搜索护工，关键词可以是城市名（如北京）或护工姓名。" +
            "可选参数：skillType按技能筛选（1临床护理 2生活照料 3康复训练 4失智护理 5居家安全 6精神慰藉），" +
            "sortBy排序（workYears按从业年限 rating按好评率）。" +
            "也可传入caregiverId直接查某个护工的完整资料。")
    public String searchCaregiverInfo(@ToolMemoryId String memoryId,
                                      String keyword,
                                      Long caregiverId,
                                      Integer skillType,
                                      String sortBy) {
        log.info("[AI Tool] 搜索护工: keyword={}, caregiverId={}, skillType={}, sortBy={}",
                keyword, caregiverId, skillType, sortBy);

        // 如果传了 caregiverId，直接返回该护工完整资料
        if (caregiverId != null && caregiverId > 0) {
            return getCaregiverFullProfile(caregiverId);
        }

        if (!StringUtils.hasText(keyword)) {
            return "请提供搜索关键词（城市名称或护工姓名），或提供护工ID查看详情。";
        }

        LambdaQueryWrapper<Caregiver> wrapper = new LambdaQueryWrapper<>();
        wrapper.and(w -> w.like(Caregiver::getRealName, keyword)
                        .or().like(Caregiver::getCityName, keyword))
                .eq(Caregiver::getVerifyStatus, 1);

        if (skillType != null && skillType > 0) {
            List<Long> caregiverIds = caregiverSkillService.getCaregiverIdsBySkillType(skillType);
            if (caregiverIds == null || caregiverIds.isEmpty()) {
                return "未找到具备该技能的护工。";
            }
            wrapper.in(Caregiver::getId, caregiverIds);
        }

        wrapper.last("LIMIT 10");
        List<Caregiver> list = caregiverMapper.selectList(wrapper);

        if (list.isEmpty()) {
            return "未找到关键词「" + keyword + "」对应的接单护工。";
        }

        // 排序处理
        if ("rating".equals(sortBy)) {
            List<Long> ids = list.stream().map(Caregiver::getId).collect(Collectors.toList());
            Map<Long, CaregiverStats> statsMap = getStatsMap(ids);
            list.sort((a, b) -> {
                BigDecimal rateA = getGoodRate(statsMap, a.getId());
                BigDecimal rateB = getGoodRate(statsMap, b.getId());
                return rateB.compareTo(rateA);
            });
        } else if ("workYears".equals(sortBy)) {
            list.sort((a, b) -> Integer.compare(
                    b.getWorkYears() != null ? b.getWorkYears() : 0,
                    a.getWorkYears() != null ? a.getWorkYears() : 0));
        }

        StringBuilder sb = new StringBuilder();
        sb.append("找到").append(list.size()).append("位护工：\n");
        for (Caregiver c : list) {
            sb.append("- ").append(c.getRealName())
                    .append("，").append(genderText(c.getGender()))
                    .append("，").append(c.getCityName())
                    .append("，从业").append(c.getWorkYears()).append("年")
                    .append("（ID:").append(c.getId()).append("）\n");
        }
        sb.append("\n您可以提供护工ID，我为您查看详细资料和评价摘要。");
        return sb.toString();
    }

    @Tool("【AI搜索】搜索用户附近的护工。自动获取用户当前位置（前端实时定位）或默认服务地址坐标，搜索附近的护工。" +
            "当用户说'附近的护工''我附近有谁''附近有哪些护工'时使用此工具，无需任何参数。")
    public String searchNearbyCaregiver(@ToolMemoryId String memoryId) {
        Long userId = resolveUserId(memoryId);
        if (userId == null) {
            return "请先登录。";
        }

        // 优先使用前端传来的实时位置信息
        LocationInfo location = memoryId != null ? CONVERSATION_LOCATION_MAP.get(memoryId) : null;
        BigDecimal lng;
        BigDecimal lat;
        String cityCode;
        String locationDesc;

        if (location != null && location.isValid()) {
            // 使用前端小程序实时定位的经纬度
            lng = location.getLongitude();
            lat = location.getLatitude();
            cityCode = location.getCityCode();
            locationDesc = "您的当前位置";
            log.info("[AI Tool] 搜索附近护工(实时定位): userId={}, lng={}, lat={}, cityCode={}", userId, lng, lat, cityCode);
        } else {
            // 回退：从用户表获取城市编码，从默认服务地址获取经纬度
            log.info("[AI Tool] 搜索附近护工(默认地址): userId={}", userId);
            UserInfoVO userInfo = userService.getUserInfo(userId);
            if (userInfo == null || !StringUtils.hasText(userInfo.getCityCode())) {
                return "请先在个人中心设置所在城市，才能搜索附近护工。";
            }
            cityCode = userInfo.getCityCode();

            List<ServiceAddress> addressList = serviceAddressService.getAddressList(userId);
            if (addressList == null || addressList.isEmpty()) {
                return "您暂未添加服务地址，请先在APP中添加常用地址后再搜索附近护工。";
            }
            ServiceAddress defaultAddr = addressList.stream()
                    .filter(a -> a.getIsDefault() != null && a.getIsDefault() == 1)
                    .findFirst()
                    .orElse(addressList.get(0));

            if (defaultAddr.getLongitude() == null || defaultAddr.getLatitude() == null) {
                return "您的默认地址未设置坐标信息，无法搜索附近护工。请在APP中重新选择地址并确保开启定位。";
            }
            lng = defaultAddr.getLongitude();
            lat = defaultAddr.getLatitude();
            locationDesc = "您的地址「" + defaultAddr.getAddress() + "」";
        }

        List<NearbyCaregiverVO> nearbyList = caregiverService.findNearbyCaregivers(cityCode, lng, lat, 10);

        if (nearbyList == null || nearbyList.isEmpty()) {
            return locationDesc + "附近暂无接单中的护工。";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("基于").append(locationDesc).append("，附近的护工（")
                .append(nearbyList.size()).append("位）：\n");
        for (NearbyCaregiverVO vo : nearbyList) {
            sb.append("- ").append(vo.getRealName())
                    .append("，距离").append(String.format("%.1f", vo.getDistanceKm())).append("km")
                    .append("，从业").append(vo.getWorkYears()).append("年")
                    .append("，好评率").append(vo.getGoodReviewRate()).append("%")
                    .append("（ID:").append(vo.getId()).append("）\n");
        }
        return sb.toString();
    }

    @Tool("【AI搜索】查询服务包。可选参数：category按类型筛选（1居家陪护 2医院陪护 3周期护理 4家政服务 5陪诊服务 6母婴护理），" +
            "keyword搜索关键词，packageId查看指定服务包详情。" +
            "当用户问'有什么服务''服务包列表''某个服务包多少钱'时使用。")
    public String searchServicePackage(Integer category, String keyword, Long packageId) {
        log.info("[AI Tool] 查询服务包: category={}, keyword={}, packageId={}", category, keyword, packageId);

        // 查看指定服务包详情
        if (packageId != null && packageId > 0) {
            ServicePackageVO v = servicePackageService.getDetailById(packageId);
            if (v == null) {
                return "未找到ID为" + packageId + "的服务包。";
            }
            StringBuilder sb = new StringBuilder();
            sb.append("服务包详情：\n");
            sb.append("- ID：").append(v.getId()).append("\n");
            sb.append("- 名称：").append(v.getName()).append("（").append(categoryName(v.getCategory())).append("）\n");
            if (StringUtils.hasText(v.getDescription())) {
                sb.append("- 简介：").append(v.getDescription()).append("\n");
            }
            if (StringUtils.hasText(v.getDetail())) {
                sb.append("- 详情：").append(v.getDetail()).append("\n");
            }
            sb.append("- 计费方式：");
            if (v.getAllowMonth() != null && v.getAllowMonth() == 1 && v.getPriceMonth() != null) {
                sb.append("按月").append(v.getPriceMonth()).append("元 ");
            }
            if (v.getAllowDay() != null && v.getAllowDay() == 1 && v.getPriceDay() != null) {
                sb.append("按天").append(v.getPriceDay()).append("元 ");
            }
            if (v.getAllowHour() != null && v.getAllowHour() == 1 && v.getPriceHour() != null) {
                sb.append("按小时").append(v.getPriceHour()).append("元 ");
            }
            if (v.getAllowTimes() != null && v.getAllowTimes() == 1 && v.getPriceTimes() != null) {
                sb.append("按次").append(v.getPriceTimes()).append("元 ");
            }
            sb.append("\n- 销量：").append(v.getSales() != null ? v.getSales() : 0).append("单");
            return sb.toString();
        }

        // 关键词搜索
        if (StringUtils.hasText(keyword)) {
            IPage<ServicePackageVO> page = servicePackageService.search(keyword, category, 1, 10);
            List<ServicePackageVO> list = page.getRecords();
            if (list.isEmpty()) {
                return "未找到与「" + keyword + "」相关的服务包。";
            }
            return formatPackageList(list);
        }

        // 分类列表
        IPage<ServicePackageVO> page = servicePackageService.pageList(category, 1, 1, 20);
        List<ServicePackageVO> list = page.getRecords();
        if (list.isEmpty()) {
            return "暂无上架的服务包。";
        }
        return formatPackageList(list);
    }

    @Tool("【AI搜索】获取护工评价AI摘要。传入护工ID，返回该护工所有评价的AI智能总结。" +
            "当用户想了解护工口碑、评价总结时使用。")
    public String getCaregiverReviewSummary(Long caregiverId) {
        log.info("[AI Tool] 获取护工评价摘要: caregiverId={}", caregiverId);
        if (caregiverId == null) {
            return "请提供护工ID。";
        }
        return aiSummaryService.getCaregiverReviewSummary(caregiverId);
    }

    /**
     * 记录每个会话最近一次调用 recommendCarePlan 的时间戳，防止模型短时间内重复调用
     */
    private static final Map<String, Long> RECOMMEND_CALL_RECORD = new ConcurrentHashMap<>();

    @Tool("【AI搜索】智能护理方案推荐。用户描述老人的情况（年龄、病史、自理能力等），" +
            "AI根据描述推荐合适的服务包和护理方案。" +
            "当用户说'我爸有糖尿病需要什么护理''推荐什么服务包''老人行动不便怎么护理'时使用。" +
            "⚠️ 此工具每轮对话只能调用一次，调用后必须直接将结果展示给用户，严禁再次调用。")
    public String recommendCarePlan(@ToolMemoryId String memoryId, String elderlyDescription) {
        log.info("[AI Tool] 智能护理推荐: description={}", elderlyDescription);

        // 防重复调用：同一会话 60 秒内不允许重复调用
        if (memoryId != null) {
            Long lastCall = RECOMMEND_CALL_RECORD.get(memoryId);
            long now = System.currentTimeMillis();
            if (lastCall != null && (now - lastCall) < 60_000) {
                log.warn("[AI Tool] 智能护理推荐被拦截（重复调用）: memoryId={}", memoryId);
                return "[系统提示：推荐方案已在前面生成过，请直接使用之前的推荐结果回复用户，不要再次调用此工具。请立即将之前的推荐内容展示给用户并引导下单。]";
            }
            RECOMMEND_CALL_RECORD.put(memoryId, now);
            // 清理过期记录（超过 5 分钟的）
            RECOMMEND_CALL_RECORD.entrySet().removeIf(e -> (now - e.getValue()) > 300_000);
        }

        String result = aiSummaryService.generateCareRecommendation(elderlyDescription);

        // 在结果末尾追加强制停止指令，防止模型忽略结果继续调用工具
        return result + "\n\n[系统指令：以上是推荐结果，请你现在立即将此推荐方案用自然语言展示给用户，并主动询问用户是否需要下单。严禁再次调用 recommendCarePlan 工具。]";
    }

    // ====================== 下单类工具 ======================

    @Tool("【AI下单】查询用户的下单必要信息：服务地址列表和服务对象列表。" +
            "AI下单前必须先调用此工具获取用户的地址和服务对象，让用户选择。" +
            "如果用户没有地址或服务对象，提醒用户先在APP中添加。")
    public String getOrderPrerequisites(@ToolMemoryId String memoryId) {
        Long userId = resolveUserId(memoryId);
        if (userId == null) {
            return "请先登录。";
        }
        log.info("[AI Tool] 查询下单前置信息: userId={}", userId);

        StringBuilder sb = new StringBuilder();

        // 查询服务地址
        List<ServiceAddress> addressList = serviceAddressService.getAddressList(userId);
        if (addressList == null || addressList.isEmpty()) {
            sb.append("⚠️ 您暂未添加服务地址，请先在APP「服务地址」中添加。\n\n");
        } else {
            sb.append("您的服务地址：\n");
            for (ServiceAddress a : addressList) {
                String addr = (a.getAddress() != null ? a.getAddress() : "");
                if (StringUtils.hasText(a.getDoorNumber())) {
                    addr = addr + " " + a.getDoorNumber();
                }
                sb.append("- 地址ID:").append(a.getId()).append(" ").append(addr);
                sb.append(" 联系人:").append(a.getContactName()).append(" ").append(a.getContactPhone());
                if (a.getIsDefault() != null && a.getIsDefault() == 1) {
                    sb.append(" [默认]");
                }
                sb.append("\n");
            }
            sb.append("\n");
        }

        // 查询服务对象
        List<ServiceSubject> subjectList = serviceSubjectService.getSubjectList(userId);
        if (subjectList == null || subjectList.isEmpty()) {
            sb.append("⚠️ 您暂未添加服务对象（被护理的老人），请先在APP「服务对象」中添加。\n");
        } else {
            sb.append("您的服务对象：\n");
            for (ServiceSubject s : subjectList) {
                sb.append("- 对象ID:").append(s.getId()).append(" ").append(s.getName());
                if (s.getRelationship() != null) {
                    sb.append("（").append(s.getRelationship()).append("）");
                }
                if (s.getBirthday() != null) {
                    int age = Period.between(s.getBirthday(), LocalDate.now()).getYears();
                    sb.append(" ").append(age).append("岁");
                }
                if (s.getGender() != null) {
                    sb.append(" ").append(s.getGender() == 1 ? "男" : "女");
                }
                if (StringUtils.hasText(s.getSelfCareAbility())) {
                    sb.append(" 自理能力:").append(s.getSelfCareAbility());
                }
                if (StringUtils.hasText(s.getMedicalHistory())) {
                    sb.append(" 病史:").append(s.getMedicalHistory());
                }
                if (s.getIsDefault() != null && s.getIsDefault() == 1) {
                    sb.append(" [默认]");
                }
                sb.append("\n");
            }
        }

        return sb.toString();
    }

    @Tool("【AI下单】为用户创建订单，支持两种模式：" +
            "1. 匹配订单（不传caregiverId）：系统自动匹配附近护工；" +
            "2. 定向预约（传caregiverId）：指定某个护工下单，适用于用户浏览护工后想直接预约该护工的服务包。" +
            "支持渐进式调用：可以只传部分参数，工具会返回当前缺少哪些信息，AI据此逐步向用户收集。" +
            "参数说明：caregiverId(护工ID，可选，传入则为定向预约)、subjectId(服务对象ID)、addressId(服务地址ID)、" +
            "packageId(服务包ID)、billingMethod(计费方式 1按月 2按天 3按小时 4按次)、buyQuantity(购买数量，默认1)、" +
            "expectStartTime(预约上门时间 yyyy-MM-dd HH:mm:ss)、specialRemark(特殊备注，可选)。" +
            "创建成功后提示用户去订单列表支付。")
    public String createOrderForUser(@ToolMemoryId String memoryId,
                                     Long caregiverId,
                                     Long subjectId,
                                     Long addressId,
                                     Long packageId,
                                     Integer billingMethod,
                                     Integer buyQuantity,
                                     String expectStartTime,
                                     String specialRemark) {
        Long userId = resolveUserId(memoryId);
        if (userId == null) {
            return "请先登录账号后再下单。";
        }
        log.info("[AI Tool] 创建订单: userId={}, caregiverId={}, subjectId={}, addressId={}, packageId={}, billingMethod={}, expectStartTime={}",
                userId, caregiverId, subjectId, addressId, packageId, billingMethod, expectStartTime);

        // ========== 渐进式参数校验：逐步引导用户补全信息 ==========
        StringBuilder missing = new StringBuilder();
        int missingCount = 0;

        if (packageId == null || packageId <= 0) {
            missing.append("- ❌ 缺少【服务包】：请先通过智能推荐或搜索服务包，让用户选择一个服务包\n");
            missingCount++;
        }
        if (subjectId == null || subjectId <= 0) {
            missing.append("- ❌ 缺少【服务对象】：请调用 getOrderPrerequisites 获取用户的服务对象列表，让用户选择\n");
            missingCount++;
        }
        if (addressId == null || addressId <= 0) {
            missing.append("- ❌ 缺少【服务地址】：请调用 getOrderPrerequisites 获取用户的服务地址列表，让用户选择\n");
            missingCount++;
        }
        if (billingMethod == null) {
            missing.append("- ❌ 缺少【计费方式】：请询问用户选择计费方式（1按月/2按天/3按小时/4按次）\n");
            missingCount++;
        }
        if (!StringUtils.hasText(expectStartTime)) {
            missing.append("- ❌ 缺少【预约上门时间】：请询问用户期望的上门服务时间（格式：yyyy-MM-dd HH:mm:ss）\n");
            missingCount++;
        }

        if (missingCount > 0) {
            return String.format("下单信息不完整，还缺少 %d 项必要信息：\n%s\n请逐步向用户收集以上信息后再次调用此工具。", missingCount, missing);
        }

        // 获取用户信息
        UserInfoVO userInfo = userService.getUserInfo(userId);
        if (userInfo == null || !StringUtils.hasText(userInfo.getPhone())) {
            return "下单失败：未能获取您的手机号，请先在个人中心完善。";
        }

        // 校验服务对象
        ServiceSubject subject;
        try {
            subject = serviceSubjectService.getSubjectDetail(userId, subjectId);
        } catch (BusinessException e) {
            return "下单失败：服务对象不存在或无权访问。";
        }
        if (subject == null) {
            return "下单失败：未找到该服务对象。";
        }

        // 校验服务地址
        ServiceAddress address;
        try {
            address = serviceAddressService.getAddressDetail(userId, addressId);
        } catch (BusinessException e) {
            return "下单失败：服务地址不存在或无权访问。";
        }
        if (address == null) {
            return "下单失败：未找到该服务地址。";
        }

        // 查询服务包
        ServicePackageVO pkg = servicePackageService.getDetailById(packageId);
        if (pkg == null) {
            return "下单失败：服务包不存在。";
        }

        // 计算年龄
        Integer age = null;
        if (subject.getBirthday() != null) {
            age = Period.between(subject.getBirthday(), LocalDate.now()).getYears();
        }

        // 根据计费方式获取单价
        BigDecimal unitPrice;
        switch (billingMethod) {
            case 1: unitPrice = pkg.getPriceMonth(); break;
            case 2: unitPrice = pkg.getPriceDay(); break;
            case 3: unitPrice = pkg.getPriceHour(); break;
            case 4: unitPrice = pkg.getPriceTimes(); break;
            default: return "下单失败：计费方式错误，请在1按月/2按天/3按小时/4按次中选择。";
        }
        if (unitPrice == null) {
            return "下单失败：该服务包不支持所选计费方式。";
        }

        int quantity = (buyQuantity != null && buyQuantity > 0) ? buyQuantity : 1;
        BigDecimal totalAmount = unitPrice.multiply(BigDecimal.valueOf(quantity));

        // 解析预约时间
        LocalDateTime expectTime;
        try {
            expectTime = LocalDateTime.parse(expectStartTime, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        } catch (Exception e) {
            expectTime = LocalDateTime.now().plusHours(2);
        }

        // 定向预约时，校验护工状态和服务包准入
        boolean isDirectOrder = (caregiverId != null && caregiverId > 0);
        Caregiver caregiver = null;
        if (isDirectOrder) {
            caregiver = caregiverService.getById(caregiverId);
            if (caregiver == null) {
                return "下单失败：未找到该护工（ID:" + caregiverId + "）。";
            }
            if (caregiver.getWorkState() == null || caregiver.getWorkState() != 1) {
                return "下单失败：该护工当前不是接单中状态，暂时无法预约。您可以选择不指定护工，由系统自动匹配。";
            }
            // 校验护工是否开通了该服务包
            if (packageId != null) {
                long hasPackage = caregiverServiceConfigService.count(
                        new LambdaQueryWrapper<CaregiverServiceConfig>()
                                .eq(CaregiverServiceConfig::getCaregiverId, caregiverId)
                                .eq(CaregiverServiceConfig::getPackageId, packageId));
                if (hasPackage == 0) {
                    return "下单失败：该护工未开通此服务包，无法定向预约。请选择该护工已开通的服务包，或不指定护工由系统匹配。";
                }
            }
        }

        // 组装公共字段
        String contactName = StringUtils.hasText(userInfo.getNickname()) ? userInfo.getNickname() : userInfo.getUsername();
        String contactPhone = userInfo.getPhone();
        String addressStr = address.getAddress() + (StringUtils.hasText(address.getDoorNumber()) ? " " + address.getDoorNumber() : "");

        try {
            Long orderId;
            if (isDirectOrder) {
                // 定向预约订单
                DirectOrderCreateDTO dto = new DirectOrderCreateDTO();
                dto.setCaregiverId(caregiverId);
                dto.setContactName(contactName);
                dto.setContactPhone(contactPhone);
                dto.setClientName(subject.getName());
                dto.setClientGender(subject.getGender());
                dto.setClientAge(age);
                dto.setClientHeight(subject.getHeight());
                dto.setClientWeight(subject.getWeight());
                dto.setIntellectStatus(subject.getIntellectStatus());
                dto.setSelfCareAbility(subject.getSelfCareAbility());
                dto.setMedicalHistory(subject.getMedicalHistory());
                dto.setRemarks(subject.getRemarks());
                dto.setAddress(address.getAddress());
                dto.setDoorNumber(address.getDoorNumber());
                dto.setLongitude(address.getLongitude());
                dto.setLatitude(address.getLatitude());
                dto.setPackageId(packageId);
                dto.setPackageName(pkg.getName());
                dto.setBillingMethod(billingMethod);
                dto.setUnitPrice(unitPrice);
                dto.setBuyQuantity(quantity);
                dto.setTotalAmount(totalAmount);
                dto.setSpecialRemark(specialRemark);
                dto.setExpectStartTime(expectTime);
                orderId = orderService.createDirectOrder(userId, dto);
                log.info("[AI Tool] 定向预约订单创建成功: userId={}, caregiverId={}, orderId={}, amount={}", userId, caregiverId, orderId, totalAmount);
                return String.format(
                        "定向预约订单创建成功！\n" +
                        "- 订单ID：%d\n" +
                        "- 指定护工：%s（ID:%d）\n" +
                        "- 服务包：%s\n" +
                        "- 服务对象：%s\n" +
                        "- 服务地址：%s\n" +
                        "- 金额：%s元（%s %s元×%d）\n" +
                        "- 预约时间：%s\n\n" +
                        "请前往【我的订单】中尽快完成支付，支付后订单将直接推送给该护工！",
                        orderId, caregiver.getRealName(), caregiverId, pkg.getName(), subject.getName(),
                        addressStr, totalAmount, billingMethodText(billingMethod), unitPrice, quantity,
                        expectTime.format(DT_FMT));
            } else {
                // 匹配订单
                MatchOrderCreateDTO dto = new MatchOrderCreateDTO();
                dto.setContactName(contactName);
                dto.setContactPhone(contactPhone);
                dto.setClientName(subject.getName());
                dto.setClientGender(subject.getGender());
                dto.setClientAge(age);
                dto.setClientHeight(subject.getHeight());
                dto.setClientWeight(subject.getWeight());
                dto.setIntellectStatus(subject.getIntellectStatus());
                dto.setSelfCareAbility(subject.getSelfCareAbility());
                dto.setMedicalHistory(subject.getMedicalHistory());
                dto.setRemarks(subject.getRemarks());
                dto.setAddress(address.getAddress());
                dto.setDoorNumber(address.getDoorNumber());
                dto.setLongitude(address.getLongitude());
                dto.setLatitude(address.getLatitude());
                dto.setPackageId(packageId);
                dto.setPackageName(pkg.getName());
                dto.setBillingMethod(billingMethod);
                dto.setUnitPrice(unitPrice);
                dto.setBuyQuantity(quantity);
                dto.setTotalAmount(totalAmount);
                dto.setSpecialRemark(specialRemark);
                dto.setExpectStartTime(expectTime);
                orderId = orderService.createMatchOrder(userId, dto);
                log.info("[AI Tool] 匹配订单创建成功: userId={}, orderId={}, amount={}", userId, orderId, totalAmount);
                return String.format(
                        "订单创建成功！\n" +
                        "- 订单ID：%d\n" +
                        "- 服务包：%s\n" +
                        "- 服务对象：%s\n" +
                        "- 服务地址：%s\n" +
                        "- 金额：%s元（%s %s元×%d）\n" +
                        "- 预约时间：%s\n\n" +
                        "请前往【我的订单】中尽快完成支付，支付后系统将自动为您匹配附近的优质护工！",
                        orderId, pkg.getName(), subject.getName(),
                        addressStr, totalAmount, billingMethodText(billingMethod), unitPrice, quantity,
                        expectTime.format(DT_FMT));
            }
        } catch (BusinessException e) {
            log.warn("[AI Tool] 订单创建失败: userId={}, caregiverId={}, error={}", userId, caregiverId, e.getMessage());
            return "下单失败：" + e.getMessage();
        }
    }

    // ====================== 私有辅助方法 ======================

    /**
     * 查询护工完整资料（基本信息 + 评价统计 + 技能 + 服务包）
     */
    private String getCaregiverFullProfile(Long caregiverId) {
        CaregiverDetailVO detail;
        try {
            detail = caregiverService.getCaregiverDetailAggregation(caregiverId);
        } catch (BusinessException e) {
            return "未找到ID为" + caregiverId + "的护工。";
        }
        if (detail == null || detail.getBasicInfo() == null) {
            return "未找到ID为" + caregiverId + "的护工。";
        }

        StringBuilder sb = new StringBuilder();
        CaregiverInfoVO info = detail.getBasicInfo();

        sb.append("【护工基本信息】\n");
        sb.append(info.getRealName());
        sb.append("，").append(genderText(info.getGender()));
        if (info.getBirthday() != null) {
            int age = Period.between(info.getBirthday(), LocalDate.now()).getYears();
            sb.append("，").append(age).append("岁");
        }
        sb.append("，学历：").append(info.getEducation() != null ? info.getEducation() : "未填写");
        sb.append("，从业").append(info.getWorkYears()).append("年");
        sb.append("，服务城市：").append(info.getCityName());
        sb.append("，工作状态：").append(workStateText(info.getWorkState()));
        sb.append("\n");

        CaregiverStatsVO stats = detail.getStats();
        if (stats != null) {
            sb.append("\n【服务评价】\n");
            sb.append("完成").append(stats.getOrderCount()).append("单");
            sb.append("，好评率").append(stats.getGoodReviewRate()).append("%");
            if (stats.getTagStats() != null && !stats.getTagStats().isEmpty()) {
                sb.append("，热门标签：");
                for (TagCountVO tag : stats.getTagStats()) {
                    sb.append(tag.getTagName()).append("(").append(tag.getCount()).append("次) ");
                }
            }
            sb.append("\n");
        }

        List<CaregiverSkillVO> skills = detail.getSkills();
        if (skills != null && !skills.isEmpty()) {
            sb.append("\n【掌握技能】\n");
            for (CaregiverSkillVO s : skills) {
                sb.append("- ").append(s.getSkillName()).append("\n");
            }
        }

        List<ServicePackageVO> packages = detail.getPackages();
        if (packages != null && !packages.isEmpty()) {
            sb.append("\n【可提供的服务】\n");
            for (ServicePackageVO p : packages) {
                sb.append("- ").append(p.getName());
                if (p.getPriceDay() != null) {
                    sb.append(" ").append(p.getPriceDay()).append("元/天");
                }
                sb.append("\n");
            }
        }

        return sb.toString();
    }

    private String formatPackageList(List<ServicePackageVO> list) {
        StringBuilder sb = new StringBuilder();
        sb.append("服务包列表（共").append(list.size()).append("个）：\n");
        for (ServicePackageVO v : list) {
            sb.append("- ").append(v.getName())
                    .append("（").append(categoryName(v.getCategory())).append("）");
            if (v.getPriceDay() != null) {
                sb.append(" ").append(v.getPriceDay()).append("元/天");
            }
            if (v.getPriceHour() != null) {
                sb.append(" ").append(v.getPriceHour()).append("元/时");
            }
            sb.append(" ID:").append(v.getId()).append("\n");
        }
        return sb.toString();
    }

    private Map<Long, CaregiverStats> getStatsMap(List<Long> caregiverIds) {
        if (caregiverIds == null || caregiverIds.isEmpty()) {
            return new HashMap<>();
        }
        List<CaregiverStats> statsList = caregiverStatsMapper.selectList(
                new LambdaQueryWrapper<CaregiverStats>().in(CaregiverStats::getCaregiverId, caregiverIds));
        Map<Long, CaregiverStats> map = new HashMap<>();
        for (CaregiverStats s : statsList) {
            map.put(s.getCaregiverId(), s);
        }
        return map;
    }

    private BigDecimal getGoodRate(Map<Long, CaregiverStats> statsMap, Long caregiverId) {
        CaregiverStats stats = statsMap.get(caregiverId);
        if (stats != null && stats.getGoodReviewRate() != null) {
            return stats.getGoodReviewRate();
        }
        return BigDecimal.ZERO;
    }

    private String genderText(Integer gender) {
        if (gender == null) {
            return "未知";
        }
        return gender == 1 ? "男" : "女";
    }

    private String workStateText(Integer state) {
        if (state == null) {
            return "未知";
        }
        switch (state) {
            case 1: return "接单中";
            case 2: return "服务中";
            case 3: return "休息中";
            default: return "未知";
        }
    }

    private String categoryName(Integer category) {
        if (category == null) {
            return "其他";
        }
        switch (category) {
            case 1: return "居家陪护";
            case 2: return "医院陪护";
            case 3: return "周期护理";
            case 4: return "家政服务";
            case 5: return "陪诊服务";
            case 6: return "母婴护理";
            default: return "其他";
        }
    }

    private String billingMethodText(Integer method) {
        if (method == null) {
            return "未知";
        }
        switch (method) {
            case 1: return "按月";
            case 2: return "按天";
            case 3: return "按小时";
            case 4: return "按次";
            default: return "未知";
        }
    }
}
