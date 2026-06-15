package com.caregiver.carelink.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.caregiver.carelink.common.exception.BusinessException;
import com.caregiver.carelink.common.result.ResultCode;
import com.caregiver.carelink.dto.SkillDictDTO;
import com.caregiver.carelink.entity.CaregiverSkill;
import com.caregiver.carelink.entity.SkillDict;
import com.caregiver.carelink.mapper.CaregiverSkillMapper;
import com.caregiver.carelink.mapper.SkillDictMapper;
import com.caregiver.carelink.service.SkillDictService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.List;

/**
 * 技能字典服务实现类
 *
 * @author CareLink
 * @since 2026-01-29
 */
@Service
public class SkillDictServiceImpl extends ServiceImpl<SkillDictMapper, SkillDict> implements SkillDictService {

    private static final Logger log = LoggerFactory.getLogger(SkillDictServiceImpl.class);

    @Resource
    private CaregiverSkillMapper caregiverSkillMapper;

    @Override
    public void addSkill(SkillDictDTO dto) {
        // 检查技能名称是否已存在
        LambdaQueryWrapper<SkillDict> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SkillDict::getSkillName, dto.getSkillName());
        if (count(wrapper) > 0) {
            throw new BusinessException("技能名称已存在");
        }

        SkillDict skill = new SkillDict();
        BeanUtil.copyProperties(dto, skill);
        save(skill);
        log.info("新增技能字典 id={}, skillName={}, skillType={}", skill.getId(), dto.getSkillName(), dto.getSkillType());
    }

    @Override
    public void updateSkill(Long skillId, SkillDictDTO dto) {
        SkillDict skill = getById(skillId);
        if (skill == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "技能不存在");
        }

        // 检查技能名称是否与其他记录重复
        LambdaQueryWrapper<SkillDict> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SkillDict::getSkillName, dto.getSkillName())
                .ne(SkillDict::getId, skillId);
        if (count(wrapper) > 0) {
            throw new BusinessException("技能名称已存在");
        }

        BeanUtil.copyProperties(dto, skill, "id");
        updateById(skill);
        log.info("更新技能字典 id={}, skillName={}", skillId, dto.getSkillName());
    }

    @Override
    public void deleteSkill(Long skillId) {
        log.info("删除技能字典 id={}", skillId);
        // 引用完整性校验：存在任何护工引用该技能（不区分审核状态）时拒绝删除，
        // 避免 caregiver_skill.skill_id 指向已失效的字典条目。
        LambdaQueryWrapper<CaregiverSkill> referenceWrapper = new LambdaQueryWrapper<>();
        referenceWrapper.eq(CaregiverSkill::getSkillId, skillId);
        Long referenceCount = caregiverSkillMapper.selectCount(referenceWrapper);
        if (referenceCount != null && referenceCount > 0) {
            throw new BusinessException("技能已被护工使用，无法删除");
        }
        removeById(skillId);
    }

    @Override
    public List<SkillDict> getSkillList(Integer skillType) {
        LambdaQueryWrapper<SkillDict> wrapper = new LambdaQueryWrapper<>();
        if (skillType != null) {
            wrapper.eq(SkillDict::getSkillType, skillType);
        }
        wrapper.orderByAsc(SkillDict::getSkillType)
                .orderByAsc(SkillDict::getId);
        return list(wrapper);
    }

    @Override
    public List<SkillDict> getAllSkills() {
        LambdaQueryWrapper<SkillDict> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByAsc(SkillDict::getSkillType)
                .orderByAsc(SkillDict::getId);
        return list(wrapper);
    }

    @Override
    public IPage<SkillDict> pageSkillList(Integer skillType, String skillNameKeyword, Long current, Long size) {
        Page<SkillDict> page = new Page<>(current, size);
        LambdaQueryWrapper<SkillDict> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(skillType != null, SkillDict::getSkillType, skillType)
                .like(StringUtils.hasText(skillNameKeyword), SkillDict::getSkillName, skillNameKeyword)
                .orderByAsc(SkillDict::getSkillType)
                .orderByAsc(SkillDict::getId);
        return page(page, wrapper);
    }

    @Override
    public List<SkillDict> searchByKeyword(String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return Collections.emptyList();
        }
        String trimKeyword = keyword.trim();
        LambdaQueryWrapper<SkillDict> wrapper = new LambdaQueryWrapper<>();
        wrapper.and(w -> w.like(SkillDict::getSkillName, trimKeyword)
                        .or()
                        .like(SkillDict::getDescription, trimKeyword))
                .orderByAsc(SkillDict::getSkillType)
                .orderByAsc(SkillDict::getId);
        return list(wrapper);
    }
}
