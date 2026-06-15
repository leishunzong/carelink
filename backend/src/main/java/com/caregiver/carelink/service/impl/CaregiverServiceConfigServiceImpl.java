package com.caregiver.carelink.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.caregiver.carelink.common.constant.RedisKeyConstants;
import com.caregiver.carelink.common.exception.BusinessException;
import com.caregiver.carelink.common.result.ResultCode;
import com.caregiver.carelink.entity.Caregiver;
import com.caregiver.carelink.entity.CaregiverServiceConfig;
import com.caregiver.carelink.entity.ServicePackage;
import com.caregiver.carelink.entity.SkillDict;
import com.caregiver.carelink.mapper.CaregiverServiceConfigMapper;
import com.caregiver.carelink.service.CaregiverService;
import com.caregiver.carelink.service.CaregiverServiceConfigService;
import com.caregiver.carelink.service.CaregiverSkillService;
import com.caregiver.carelink.service.ServicePackageService;
import com.caregiver.carelink.service.SkillDictService;
import com.caregiver.carelink.utils.RedisUtils;
import com.caregiver.carelink.vo.CaregiverSkillVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 护工服务包准入关联服务实现：维护 DB 与 Redis（服务包 -> 可接单护工 ID 集合）
 *
 * @author CareLink
 * @since 2026-02-11
 */
@Service
public class CaregiverServiceConfigServiceImpl extends ServiceImpl<CaregiverServiceConfigMapper, CaregiverServiceConfig> implements CaregiverServiceConfigService {

    private static final Logger log = LoggerFactory.getLogger(CaregiverServiceConfigServiceImpl.class);

    @Resource
    private CaregiverService caregiverService;

    @Resource
    private ServicePackageService servicePackageService;

    @Resource
    private CaregiverSkillService caregiverSkillService;

    @Resource
    private SkillDictService skillDictService;

    @Resource
    private RedisUtils redisUtils;

    @Override
    public void addAssociation(Long caregiverId, Long packageId) {
        Caregiver caregiver = caregiverService.getById(caregiverId);
        if (caregiver == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "护工不存在");
        }
        ServicePackage servicePackage = servicePackageService.getById(packageId);
        if (servicePackage == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "服务包不存在");
        }

        // 护工技能必须包含服务包要求的全部技能标签（仅统计审核通过的技能）
        List<Long> requiredSkillIds = parseMandatorySkills(servicePackage.getMandatorySkills());
        if (!CollectionUtils.isEmpty(requiredSkillIds)) {
            List<CaregiverSkillVO> caregiverSkills = caregiverSkillService.getSkillList(caregiverId);
            Set<Long> caregiverSkillIds = caregiverSkills.stream()
                    .filter(s -> s.getAuditStatus() != null && s.getAuditStatus() == 1)
                    .map(CaregiverSkillVO::getSkillId)
                    .collect(Collectors.toSet());
            List<Long> missingIds = requiredSkillIds.stream()
                    .filter(id -> !caregiverSkillIds.contains(id))
                    .collect(Collectors.toList());
            if (!missingIds.isEmpty()) {
                List<SkillDict> missingSkills = skillDictService.listByIds(missingIds);
                String missingNames = missingSkills.stream()
                        .map(SkillDict::getSkillName)
                        .collect(Collectors.joining("、"));
                throw new BusinessException("护工技能不满足该服务包要求，请先添加以下技能：" + missingNames);
            }
        }

        LambdaQueryWrapper<CaregiverServiceConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CaregiverServiceConfig::getCaregiverId, caregiverId)
                .eq(CaregiverServiceConfig::getPackageId, packageId);
        if (count(wrapper) > 0) {
            throw new BusinessException("已关联该服务包");
        }

        CaregiverServiceConfig config = new CaregiverServiceConfig();
        config.setCaregiverId(caregiverId);
        config.setPackageId(packageId);
        save(config);

        String redisKey = RedisKeyConstants.getPackageCaregiversKey(packageId);
        redisUtils.sSet(redisKey, caregiverId);
        log.info("护工关联服务包成功 caregiverId={}, packageId={}", caregiverId, packageId);
    }

    @Override
    public void removeAssociation(Long caregiverId, Long packageId) {
        log.info("护工解除服务包关联 caregiverId={}, packageId={}", caregiverId, packageId);
        LambdaQueryWrapper<CaregiverServiceConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CaregiverServiceConfig::getCaregiverId, caregiverId)
                .eq(CaregiverServiceConfig::getPackageId, packageId);
        CaregiverServiceConfig config = getOne(wrapper);
        if (config == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "未关联该服务包");
        }

        removeById(config.getId());

        String redisKey = RedisKeyConstants.getPackageCaregiversKey(packageId);
        redisUtils.setRemove(redisKey, caregiverId);
        log.info("护工解除服务包关联成功 caregiverId={}, packageId={}", caregiverId, packageId);
    }

    /**
     * 解析服务包要求的技能 ID 列表（逗号分隔字符串 -> List）
     */
    private List<Long> parseMandatorySkills(String mandatorySkills) {
        if (!StringUtils.hasText(mandatorySkills)) {
            return Collections.emptyList();
        }
        return Stream.of(mandatorySkills.split(","))
                .filter(s -> !s.trim().isEmpty())
                .map(s -> Long.parseLong(s.trim()))
                .collect(Collectors.toList());
    }
}
