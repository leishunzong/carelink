package com.caregiver.carelink.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.caregiver.carelink.common.constant.RedisKeyConstants;
import com.caregiver.carelink.common.constant.WorkStateConstants;
import com.caregiver.carelink.common.exception.BusinessException;
import com.caregiver.carelink.common.result.ResultCode;
import com.caregiver.carelink.common.constant.VerifyMaterialTypeConstants;
import com.caregiver.carelink.dto.CaregiverRegisterDTO;
import com.caregiver.carelink.dto.CaregiverSearchDTO;
import com.caregiver.carelink.dto.CaregiverSettleDTO;
import com.caregiver.carelink.dto.CaregiverUpdateDTO;
import com.caregiver.carelink.dto.LoginDTO;
import com.caregiver.carelink.entity.Caregiver;
import com.caregiver.carelink.entity.CaregiverStats;
import com.caregiver.carelink.entity.CaregiverVerifyMaterial;
import com.caregiver.carelink.mapper.CaregiverMapper;
import com.caregiver.carelink.mapper.CaregiverStatsMapper;
import com.caregiver.carelink.mapper.CaregiverVerifyMaterialMapper;
import com.caregiver.carelink.service.CaregiverService;
import com.caregiver.carelink.service.CaregiverSkillService;
import com.caregiver.carelink.service.ServicePackageService;
import com.caregiver.carelink.service.StatsService;
import com.caregiver.carelink.utils.JwtUtils;
import com.caregiver.carelink.utils.PasswordUtils;
import com.caregiver.carelink.utils.RedisUtils;
import com.caregiver.carelink.vo.CaregiverDetailVO;
import com.caregiver.carelink.vo.CaregiverInfoVO;
import com.caregiver.carelink.vo.CaregiverSkillVO;
import com.caregiver.carelink.vo.CaregiverStatsVO;
import com.caregiver.carelink.vo.LoginVO;
import com.caregiver.carelink.vo.NearbyCaregiverVO;
import com.caregiver.carelink.vo.ServicePackageVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.geo.GeoResult;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Collections;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/**
 * 护工服务实现类
 *
 * @author CareLink
 * @since 2026-01-29
 */
@Service
public class CaregiverServiceImpl extends ServiceImpl<CaregiverMapper, Caregiver> implements CaregiverService {

    private static final Logger log = LoggerFactory.getLogger(CaregiverServiceImpl.class);

    @Resource
    private JwtUtils jwtUtils;

    @Resource
    private RedisUtils redisUtils;

    @Resource
    private CaregiverVerifyMaterialMapper caregiverVerifyMaterialMapper;

    @Resource
    private CaregiverStatsMapper caregiverStatsMapper;

    @Resource
    private CaregiverSkillService caregiverSkillService;

    @Resource
    private ServicePackageService servicePackageService;

    @Resource
    private StatsService statsService;

    @Override
    public void register(CaregiverRegisterDTO dto) {
        log.info("护工注册 username={}, phone={}", dto.getUsername(), dto.getPhone());
        // 检查用户名是否已存在
        LambdaQueryWrapper<Caregiver> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Caregiver::getUsername, dto.getUsername());
        if (count(wrapper) > 0) {
            throw new BusinessException("用户名已存在");
        }

        // 检查手机号是否已被注册
        wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Caregiver::getPhone, dto.getPhone());
        if (count(wrapper) > 0) {
            throw new BusinessException("手机号已被注册");
        }

        // 创建护工
        Caregiver caregiver = new Caregiver();
        caregiver.setUsername(dto.getUsername());
        caregiver.setPassword(PasswordUtils.encode(dto.getPassword()));
        caregiver.setPhone(dto.getPhone());
        caregiver.setRealName(dto.getRealName());
        caregiver.setGender(dto.getGender());
        caregiver.setVerifyStatus(0); // 待审核
        caregiver.setWorkState(WorkStateConstants.WORK_STATE_REST); // 默认休息中

        save(caregiver);
        log.info("护工注册成功 caregiverId={}, username={}", caregiver.getId(), dto.getUsername());
    }

    @Override
    public LoginVO login(LoginDTO dto) {
        log.info("护工登录 username={}", dto.getUsername());
        // 查询护工
        LambdaQueryWrapper<Caregiver> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Caregiver::getUsername, dto.getUsername())
                .or()
                .eq(Caregiver::getPhone, dto.getUsername());
        Caregiver caregiver = getOne(wrapper);

        if (caregiver == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "用户名或密码错误");
        }

        // 验证密码
        if (!PasswordUtils.matches(dto.getPassword(), caregiver.getPassword())) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "用户名或密码错误");
        }

        // 检查审核状态
        if (caregiver.getVerifyStatus() == 2) {
            throw new BusinessException(ResultCode.FORBIDDEN, "账号审核未通过");
        }

        // 生成Token
        Map<String, Object> claims = new HashMap<>();
        claims.put("caregiverId", caregiver.getId());
        claims.put("userType", "caregiver");
        String token = jwtUtils.generateToken(claims);

        // 返回登录信息
        LoginVO loginVO = LoginVO.builder()
                .token(token)
                .userType("caregiver")
                .build();
        log.info("护工登录成功 caregiverId={}, verifyStatus={}", caregiver.getId(), caregiver.getVerifyStatus());
        return loginVO;
    }

    @Override
    public CaregiverInfoVO getCaregiverInfo(Long caregiverId) {
        Caregiver caregiver = getById(caregiverId);
        if (caregiver == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "护工不存在");
        }

        CaregiverInfoVO vo = new CaregiverInfoVO();
        BeanUtil.copyProperties(caregiver, vo);
        return vo;
    }

    @Override
    public CaregiverDetailVO getCaregiverDetailAggregation(Long caregiverId) {
        // 异步编排：基础信息、技能、服务包、统计并行查询，互不阻塞
        CompletableFuture<CaregiverInfoVO> fBasic = CompletableFuture.supplyAsync(() -> getCaregiverInfo(caregiverId));
        CompletableFuture<List<CaregiverSkillVO>> fSkills = CompletableFuture.supplyAsync(() -> caregiverSkillService.getSkillList(caregiverId));
        CompletableFuture<List<ServicePackageVO>> fPackages = CompletableFuture.supplyAsync(() -> servicePackageService.getCaregiverPackages(caregiverId));
        CompletableFuture<CaregiverStatsVO> fStats = CompletableFuture.supplyAsync(() -> statsService.getCaregiverStats(caregiverId));

        CaregiverDetailVO vo = new CaregiverDetailVO();
        vo.setBasicInfo(fBasic.join());
        vo.setSkills(fSkills.join());
        vo.setPackages(fPackages.join());
        vo.setStats(fStats.join());
        return vo;
    }

    @Override
    public void settle(Long caregiverId, CaregiverSettleDTO dto) {
        log.info("护工入驻提交 caregiverId={}", caregiverId);
        Caregiver caregiver = getById(caregiverId);
        if (caregiver == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "护工不存在");
        }
        // 仅待审核(0)或已拒绝(2)可入驻/重新提交；已通过(1)请使用修改信息接口
        Integer vs = caregiver.getVerifyStatus();
        if (vs == null || (vs != 0 && vs != 2)) {
            throw new BusinessException("仅待审核或审核被拒状态可提交入驻，审核通过后请使用修改信息接口");
        }
        // 入驻/重新提交：补齐护工表基本信息（不拷贝证件URL等字段；realName 等由前端在入驻时传入）
        BeanUtil.copyProperties(dto, caregiver, "id", "username", "password", "phone", "verifyStatus", "workState",
                "idCardFrontUrl", "idCardBackUrl", "qualificationCertUrl", "otherMaterialUrls");
        // 若是审核被拒后重新提交，将状态改回待审核，等待再次审核
        if (vs != null && vs == 2) {
            caregiver.setVerifyStatus(0);
        }
        updateById(caregiver);

        // 审核材料：先删后插
        caregiverVerifyMaterialMapper.delete(new LambdaQueryWrapper<CaregiverVerifyMaterial>()
                .eq(CaregiverVerifyMaterial::getCaregiverId, caregiverId));
        List<CaregiverVerifyMaterial> materials = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        if (StringUtils.hasText(dto.getIdCardFrontUrl())) {
            CaregiverVerifyMaterial m = new CaregiverVerifyMaterial();
            m.setCaregiverId(caregiverId);
            m.setMaterialType(VerifyMaterialTypeConstants.TYPE_ID_CARD_FRONT);
            m.setFileUrl(dto.getIdCardFrontUrl());
            m.setSortOrder(0);
            m.setCreateTime(now);
            materials.add(m);
        }
        if (StringUtils.hasText(dto.getIdCardBackUrl())) {
            CaregiverVerifyMaterial m = new CaregiverVerifyMaterial();
            m.setCaregiverId(caregiverId);
            m.setMaterialType(VerifyMaterialTypeConstants.TYPE_ID_CARD_BACK);
            m.setFileUrl(dto.getIdCardBackUrl());
            m.setSortOrder(0);
            m.setCreateTime(now);
            materials.add(m);
        }
        if (StringUtils.hasText(dto.getQualificationCertUrl())) {
            CaregiverVerifyMaterial m = new CaregiverVerifyMaterial();
            m.setCaregiverId(caregiverId);
            m.setMaterialType(VerifyMaterialTypeConstants.TYPE_QUALIFICATION_CERT);
            m.setFileUrl(dto.getQualificationCertUrl());
            m.setSortOrder(0);
            m.setCreateTime(now);
            materials.add(m);
        }
        if (!CollectionUtils.isEmpty(dto.getOtherMaterialUrls())) {
            int order = 0;
            for (String url : dto.getOtherMaterialUrls()) {
                if (!StringUtils.hasText(url)) continue;
                CaregiverVerifyMaterial m = new CaregiverVerifyMaterial();
                m.setCaregiverId(caregiverId);
                m.setMaterialType(VerifyMaterialTypeConstants.TYPE_OTHER);
                m.setFileUrl(url);
                m.setSortOrder(order++);
                m.setCreateTime(now);
                materials.add(m);
            }
        }
        for (CaregiverVerifyMaterial m : materials) {
            caregiverVerifyMaterialMapper.insert(m);
        }
        log.info("护工入驻提交成功 caregiverId={}, 材料数={}", caregiverId, materials.size());
    }

    @Override
    public void updateCaregiverInfo(Long caregiverId, CaregiverUpdateDTO dto) {
        Caregiver caregiver = getById(caregiverId);
        if (caregiver == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "护工不存在");
        }

        String oldCityCode = caregiver.getCityCode();
        Integer workState = caregiver.getWorkState();

        // 更新护工信息（排除不能修改的字段：ID、账号信息、审核状态、工作状态）
        BeanUtil.copyProperties(dto, caregiver, "id", "username", "password", "phone", "realName", "verifyStatus", "workState");

        updateById(caregiver);

        // 如果修改了城市且当前是接单中状态，需要更新Redis GEO
        if (dto.getCityCode() != null && !dto.getCityCode().equals(oldCityCode) 
            && WorkStateConstants.WORK_STATE_AVAILABLE.equals(workState)) {
            // 从旧城市的Redis GEO删除
            if (oldCityCode != null && !oldCityCode.isEmpty()) {
                String oldGeoKey = RedisKeyConstants.getCaregiverLocationGeoKey(oldCityCode);
                String member = RedisKeyConstants.getCaregiverGeoMember(caregiverId);
                redisUtils.geoRemove(oldGeoKey, member);
                log.info("护工{}更换城市，已从旧城市{}的Redis GEO删除", caregiverId, oldCityCode);
            }

            // 添加到新城市的Redis GEO
            if (caregiver.getLongitude() != null && caregiver.getLatitude() != null) {
                String newGeoKey = RedisKeyConstants.getCaregiverLocationGeoKey(dto.getCityCode());
                String member = RedisKeyConstants.getCaregiverGeoMember(caregiverId);
                redisUtils.geoAdd(newGeoKey, caregiver.getLongitude().doubleValue(), 
                        caregiver.getLatitude().doubleValue(), member);
                log.info("护工{}更换城市，已添加到新城市{}的Redis GEO", caregiverId, dto.getCityCode());
            }
        }
    }

    @Override
    public void updateLocation(Long caregiverId, BigDecimal longitude, BigDecimal latitude) {
        Caregiver caregiver = getById(caregiverId);
        if (caregiver == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "护工不存在");
        }

        caregiver.setLongitude(longitude);
        caregiver.setLatitude(latitude);
        updateById(caregiver);

        // 如果护工当前是接单中状态，同步更新Redis GEO位置
        if (WorkStateConstants.WORK_STATE_AVAILABLE.equals(caregiver.getWorkState()) 
            && caregiver.getCityCode() != null && !caregiver.getCityCode().isEmpty()) {
            String geoKey = RedisKeyConstants.getCaregiverLocationGeoKey(caregiver.getCityCode());
            String member = RedisKeyConstants.getCaregiverGeoMember(caregiverId);
            redisUtils.geoAdd(geoKey, longitude.doubleValue(), latitude.doubleValue(), member);
            log.info("护工{}位置已更新到城市{}的Redis GEO", caregiverId, caregiver.getCityCode());
        }
    }

    @Override
    public void updateWorkState(Long caregiverId, Integer workState) {
        Caregiver caregiver = getById(caregiverId);
        if (caregiver == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "护工不存在");
        }

        if (caregiver.getCityCode() == null || caregiver.getCityCode().isEmpty()) {
            throw new BusinessException("护工城市信息不完整");
        }

        // 验证状态值（只能切换接单中或休息中）
        if (workState == null ||
                (workState != WorkStateConstants.WORK_STATE_AVAILABLE &&
                        workState != WorkStateConstants.WORK_STATE_REST)) {
            throw new BusinessException("工作状态值错误，只能是 1(接单中) 或 3(休息中)");
        }

        // 如果当前正在服务中，不允许切换
        if (WorkStateConstants.WORK_STATE_BUSY.equals(caregiver.getWorkState())) {
            throw new BusinessException("当前正在服务中，无法切换状态");
        }

        BigDecimal longitude = caregiver.getLongitude();
        BigDecimal latitude = caregiver.getLatitude();
        // 检查经纬度是否存在
        if (longitude == null || latitude == null) {
            throw new BusinessException("请先设置常驻地址的经纬度信息");
        }

        caregiver.setWorkState(workState);
        updateById(caregiver);

        // 同步更新Redis GEO
        syncCaregiverLocationToRedis(caregiver, workState, longitude, latitude);
    }

    /**
     * 同步护工位置到Redis GEO
     *
     * @param caregiver   护工
     * @param newState    新状态
     * @param longitude   经度
     * @param latitude    纬度
     */
    private void syncCaregiverLocationToRedis(Caregiver caregiver, Integer newState, BigDecimal longitude, BigDecimal latitude) {

        String geoKey = RedisKeyConstants.getCaregiverLocationGeoKey(caregiver.getCityCode());
        String member = RedisKeyConstants.getCaregiverGeoMember(caregiver.getId());

        // 如果切换为接单中，添加到该城市的Redis GEO
        if (WorkStateConstants.WORK_STATE_AVAILABLE.equals(newState)) {
            Long result = redisUtils.geoAdd(geoKey, longitude.doubleValue(), latitude.doubleValue(), member);
            log.info("护工{}切换为接单中，已添加到城市{}的Redis GEO, result={}", 
                    caregiver.getId(), caregiver.getCityCode(), result);
        }
        // 如果切换为休息中，从该城市的Redis GEO删除
        else if (WorkStateConstants.WORK_STATE_REST.equals(newState)) {
            Long result = redisUtils.geoRemove(geoKey, member);
            log.info("护工{}切换为休息中，已从城市{}的Redis GEO删除, result={}", 
                    caregiver.getId(), caregiver.getCityCode(), result);
        }
    }

    @Override
    public void updatePassword(Long caregiverId, String oldPassword, String newPassword) {
        log.info("护工修改密码 caregiverId={}", caregiverId);
        Caregiver caregiver = getById(caregiverId);
        if (caregiver == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "护工不存在");
        }

        // 验证旧密码
        if (!PasswordUtils.matches(oldPassword, caregiver.getPassword())) {
            throw new BusinessException("旧密码错误");
        }

        // 更新为新密码
        caregiver.setPassword(PasswordUtils.encode(newPassword));
        updateById(caregiver);
        log.info("护工修改密码成功 caregiverId={}", caregiverId);
    }

    @Override
    public Page<CaregiverInfoVO> searchCaregivers(CaregiverSearchDTO dto) {
        // 计算年龄对应的生日范围
        LocalDate minBirthday = null;
        LocalDate maxBirthday = null;
        if (dto.getMinAge() != null || dto.getMaxAge() != null) {
            LocalDate now = LocalDate.now();
            if (dto.getMaxAge() != null) {
                minBirthday = now.minusYears(dto.getMaxAge() + 1);
            }
            if (dto.getMinAge() != null) {
                maxBirthday = now.minusYears(dto.getMinAge());
            }
        }

        // 分页查询（使用自定义SQL，支持关联统计表排序）
        Page<Caregiver> page = new Page<>(dto.getPage(), dto.getSize());
        page = baseMapper.searchCaregivers(
                page,
                dto.getCityCode(),
                dto.getGender(),
                dto.getNameKeyword(),
                minBirthday,
                maxBirthday,
                dto.getMinWorkYears(),
                dto.getMaxWorkYears(),
                dto.getEducation(),
                dto.getPackageCategory(),
                dto.getSortField(),
                dto.getSortOrder()
        );

        // 转换为 VO
        Page<CaregiverInfoVO> voPage = new Page<>();
        BeanUtil.copyProperties(page, voPage, "records");
        
        List<CaregiverInfoVO> voList = new ArrayList<>();
        for (Caregiver caregiver : page.getRecords()) {
            CaregiverInfoVO vo = new CaregiverInfoVO();
            BeanUtil.copyProperties(caregiver, vo);
            voList.add(vo);
        }
        voPage.setRecords(voList);

        return voPage;
    }

    @Override
    public IPage<CaregiverInfoVO> pageCaregiversForAdmin(String realNameKeyword, String phoneKeyword, Integer gender,
                                                         Integer minAge, Integer maxAge, String education, Integer workYears,
                                                         String cityNameKeyword, Integer workState, Long current, Long size) {
        LambdaQueryWrapper<Caregiver> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StringUtils.hasText(realNameKeyword), Caregiver::getRealName, realNameKeyword)
                .like(StringUtils.hasText(phoneKeyword), Caregiver::getPhone, phoneKeyword)
                .eq(gender != null, Caregiver::getGender, gender)
                .apply(minAge != null, "(YEAR(CURDATE())-YEAR(birthday)) >= {0}", minAge)
                .apply(maxAge != null, "(YEAR(CURDATE())-YEAR(birthday)) <= {0}", maxAge)
                .like(StringUtils.hasText(education), Caregiver::getEducation, education)
                .eq(workYears != null, Caregiver::getWorkYears, workYears)
                .like(StringUtils.hasText(cityNameKeyword), Caregiver::getCityName, cityNameKeyword)
                .eq(workState != null, Caregiver::getWorkState, workState)
                .orderByDesc(Caregiver::getCreateTime);
        Page<Caregiver> p = page(new Page<>(current, size), wrapper);
        Page<CaregiverInfoVO> voPage = new Page<>(p.getCurrent(), p.getSize(), p.getTotal());
        List<CaregiverInfoVO> voList = new ArrayList<>();
        for (Caregiver c : p.getRecords()) {
            CaregiverInfoVO vo = new CaregiverInfoVO();
            BeanUtil.copyProperties(c, vo);
            voList.add(vo);
        }
        voPage.setRecords(voList);
        return voPage;
    }

    private static final double DEFAULT_NEARBY_RADIUS_KM = 30.0;

    @Override
    public List<NearbyCaregiverVO> findNearbyCaregivers(String cityCode, BigDecimal longitude, BigDecimal latitude,
                                                        Integer limit) {
        if (!StringUtils.hasText(cityCode)) {
            throw new BusinessException("城市编码不能为空");
        }
        if (longitude == null || latitude == null) {
            throw new BusinessException("经度和纬度不能为空");
        }
        double radius = DEFAULT_NEARBY_RADIUS_KM;
        long count = (limit != null && limit > 0 && limit <= 50) ? limit : 20;

        String geoKey = RedisKeyConstants.getCaregiverLocationGeoKey(cityCode);
        List<GeoResult<RedisGeoCommands.GeoLocation<Object>>> results =
                redisUtils.geoSearchNearby(geoKey, longitude.doubleValue(), latitude.doubleValue(), radius, count);

        if (results.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> caregiverIds = new ArrayList<>();
        for (GeoResult<RedisGeoCommands.GeoLocation<Object>> result : results) {
            Object name = result.getContent().getName();
            if (name == null) {
                continue;
            }
            try {
                caregiverIds.add(Long.parseLong(name.toString()));
            } catch (NumberFormatException ignored) {
            }
        }
        if (caregiverIds.isEmpty()) {
            return Collections.emptyList();
        }

        List<Caregiver> caregivers = listByIds(caregiverIds);
        if (caregivers.isEmpty()) {
            return Collections.emptyList();
        }

        Map<Long, Caregiver> caregiverMap = new HashMap<>();
        for (Caregiver c : caregivers) {
            // 仅返回审核通过且接单中的护工
            if (c.getVerifyStatus() != null && c.getVerifyStatus() == 1
                    && WorkStateConstants.WORK_STATE_AVAILABLE.equals(c.getWorkState())) {
                caregiverMap.put(c.getId(), c);
            }
        }
        if (caregiverMap.isEmpty()) {
            return Collections.emptyList();
        }

        List<CaregiverStats> statsList = caregiverStatsMapper.selectList(
                new LambdaQueryWrapper<CaregiverStats>()
                        .in(CaregiverStats::getCaregiverId, caregiverMap.keySet()));
        Map<Long, CaregiverStats> statsMap = new HashMap<>();
        for (CaregiverStats s : statsList) {
            statsMap.put(s.getCaregiverId(), s);
        }

        List<NearbyCaregiverVO> voList = new ArrayList<>();
        for (GeoResult<RedisGeoCommands.GeoLocation<Object>> result : results) {
            Object name = result.getContent().getName();
            if (name == null) {
                continue;
            }
            Long id;
            try {
                id = Long.parseLong(name.toString());
            } catch (NumberFormatException e) {
                continue;
            }
            Caregiver c = caregiverMap.get(id);
            if (c == null) {
                continue;
            }
            NearbyCaregiverVO vo = new NearbyCaregiverVO();
            vo.setId(c.getId());
            vo.setRealName(c.getRealName());
            vo.setAvatar(c.getAvatar());
            vo.setWorkYears(c.getWorkYears());
            if (result.getDistance() != null) {
                vo.setDistanceKm(result.getDistance().getValue());
            }
            CaregiverStats stats = statsMap.get(id);
            if (stats != null) {
                vo.setOrderCount(stats.getOrderCount() != null ? stats.getOrderCount() : 0);
                vo.setGoodReviewRate(stats.getGoodReviewRate() != null ? stats.getGoodReviewRate() : BigDecimal.ZERO);
                if (stats.getReviewCount() != null && stats.getReviewCount() > 0
                        && stats.getStarRatingSum() != null && stats.getStarRatingSum() > 0) {
                    vo.setAverageRating(new BigDecimal(stats.getStarRatingSum())
                            .divide(BigDecimal.valueOf(stats.getReviewCount()), 1, RoundingMode.HALF_UP));
                } else {
                    vo.setAverageRating(BigDecimal.ZERO);
                }
            } else {
                vo.setOrderCount(0);
                vo.setGoodReviewRate(BigDecimal.ZERO);
                vo.setAverageRating(BigDecimal.ZERO);
            }
            voList.add(vo);
        }
        return voList;
    }
}
