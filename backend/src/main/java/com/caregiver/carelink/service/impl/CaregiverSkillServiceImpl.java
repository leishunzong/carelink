package com.caregiver.carelink.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.caregiver.carelink.common.exception.BusinessException;
import com.caregiver.carelink.common.result.ResultCode;
import com.caregiver.carelink.dto.CaregiverSkillDTO;
import com.caregiver.carelink.entity.CaregiverSkill;
import com.caregiver.carelink.entity.SkillDict;
import com.caregiver.carelink.mapper.CaregiverSkillMapper;
import com.caregiver.carelink.service.CaregiverSkillService;
import com.caregiver.carelink.service.SkillDictService;
import com.caregiver.carelink.vo.CaregiverSkillVO;
import com.caregiver.carelink.vo.SkillDictWithStatusVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 护工技能服务实现类
 *
 * @author CareLink
 * @since 2026-01-29
 */
@Service
public class CaregiverSkillServiceImpl extends ServiceImpl<CaregiverSkillMapper, CaregiverSkill> implements CaregiverSkillService {

    private static final Logger log = LoggerFactory.getLogger(CaregiverSkillServiceImpl.class);

    @Resource
    private SkillDictService skillDictService;

    @Override
    public void addSkill(Long caregiverId, CaregiverSkillDTO dto) {
        log.info("护工添加技能 caregiverId={}, skillId={}", caregiverId, dto.getSkillId());
        // 检查技能字典是否存在
        SkillDict skillDict = skillDictService.getById(dto.getSkillId());
        if (skillDict == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "技能不存在");
        }

        // 检查是否已添加该技能
        LambdaQueryWrapper<CaregiverSkill> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CaregiverSkill::getCaregiverId, caregiverId)
                .eq(CaregiverSkill::getSkillId, dto.getSkillId());
        if (count(wrapper) > 0) {
            throw new BusinessException("该技能已添加");
        }

        CaregiverSkill skill = new CaregiverSkill();
        BeanUtil.copyProperties(dto, skill);
        skill.setCaregiverId(caregiverId);
        // 根据技能是否需审核设置审核状态：需审核则待审核(0)，否则默认通过(1)
        skill.setAuditStatus((skillDict.getNeedAudit() != null && skillDict.getNeedAudit() == 1) ? 0 : 1);
        save(skill);
        log.info("护工添加技能成功 caregiverId={}, skillId={}, auditStatus={}", caregiverId, dto.getSkillId(), skill.getAuditStatus());
    }

    @Override
    public void deleteSkillBySkillId(Long caregiverId, Long skillId) {
        log.info("护工删除技能 caregiverId={}, skillId={}", caregiverId, skillId);
        // 根据 caregiverId 和 skillId 组合删除
        LambdaQueryWrapper<CaregiverSkill> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CaregiverSkill::getCaregiverId, caregiverId)
                .eq(CaregiverSkill::getSkillId, skillId);
        
        CaregiverSkill skill = getOne(wrapper);
        if (skill == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "未找到该技能");
        }

        removeById(skill.getId());
    }

    @Override
    public List<CaregiverSkillVO> getSkillList(Long caregiverId) {
        // 查询护工的技能
        LambdaQueryWrapper<CaregiverSkill> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CaregiverSkill::getCaregiverId, caregiverId)
                .orderByAsc(CaregiverSkill::getId);
        List<CaregiverSkill> skillList = list(wrapper);

        // 关联查询技能字典信息
        List<CaregiverSkillVO> voList = new ArrayList<>();
        for (CaregiverSkill skill : skillList) {
            SkillDict skillDict = skillDictService.getById(skill.getSkillId());
            if (skillDict != null) {
                CaregiverSkillVO vo = new CaregiverSkillVO();
                BeanUtil.copyProperties(skill, vo);
                vo.setSkillName(skillDict.getSkillName());
                vo.setSkillType(skillDict.getSkillType());
                vo.setDescription(skillDict.getDescription());
                vo.setAuditStatus(skill.getAuditStatus());
                voList.add(vo);
            }
        }

        return voList;
    }

    @Override
    public List<Long> getCaregiverIdsBySkillType(Integer skillType) {
        if (skillType == null) {
            return new ArrayList<>();
        }
        // 先查该类型下的技能字典ID
        List<SkillDict> skillDicts = skillDictService.getSkillList(skillType);
        if (skillDicts == null || skillDicts.isEmpty()) {
            return new ArrayList<>();
        }
        List<Long> skillIds = new ArrayList<>();
        for (SkillDict sd : skillDicts) {
            skillIds.add(sd.getId());
        }

        // 查拥有这些技能且审核通过的护工ID（去重）
        LambdaQueryWrapper<CaregiverSkill> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(CaregiverSkill::getSkillId, skillIds)
                .eq(CaregiverSkill::getAuditStatus, 1)
                .select(CaregiverSkill::getCaregiverId)
                .groupBy(CaregiverSkill::getCaregiverId);
        List<CaregiverSkill> skills = list(wrapper);

        List<Long> caregiverIds = new ArrayList<>();
        for (CaregiverSkill s : skills) {
            caregiverIds.add(s.getCaregiverId());
        }
        return caregiverIds;
    }

    @Override
    public List<SkillDictWithStatusVO> getAllSkillsWithStatus(Long caregiverId, Integer skillType) {
        // 查询技能字典
        List<SkillDict> allSkills = skillType != null
                ? skillDictService.getSkillList(skillType)
                : skillDictService.getAllSkills();

        // 查询护工已申请的技能，建立 skillId -> CaregiverSkill 映射
        LambdaQueryWrapper<CaregiverSkill> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CaregiverSkill::getCaregiverId, caregiverId);
        List<CaregiverSkill> mySkills = list(wrapper);
        Map<Long, CaregiverSkill> mySkillMap = new HashMap<>();
        for (CaregiverSkill cs : mySkills) {
            mySkillMap.put(cs.getSkillId(), cs);
        }

        // 组装结果
        List<SkillDictWithStatusVO> result = new ArrayList<>();
        for (SkillDict dict : allSkills) {
            SkillDictWithStatusVO vo = new SkillDictWithStatusVO();
            vo.setId(dict.getId());
            vo.setSkillName(dict.getSkillName());
            vo.setSkillType(dict.getSkillType());
            vo.setDescription(dict.getDescription());
            vo.setNeedAudit(dict.getNeedAudit());

            CaregiverSkill applied = mySkillMap.get(dict.getId());
            if (applied != null) {
                vo.setApplied(true);
                vo.setAuditStatus(applied.getAuditStatus());
                vo.setCertImage(applied.getCertImage());
                vo.setApplyTime(applied.getCreateTime());
            } else {
                vo.setApplied(false);
            }
            result.add(vo);
        }
        return result;
    }
}

