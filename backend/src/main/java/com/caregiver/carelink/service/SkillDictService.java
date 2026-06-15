package com.caregiver.carelink.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.caregiver.carelink.dto.SkillDictDTO;
import com.caregiver.carelink.entity.SkillDict;

import java.util.List;

/**
 * 技能字典服务接口
 *
 * @author CareLink
 * @since 2026-01-29
 */
public interface SkillDictService extends IService<SkillDict> {

    /**
     * 新增技能
     */
    void addSkill(SkillDictDTO dto);

    /**
     * 修改技能
     */
    void updateSkill(Long skillId, SkillDictDTO dto);

    /**
     * 删除技能
     */
    void deleteSkill(Long skillId);

    /**
     * 查询技能列表（按分类）
     */
    List<SkillDict> getSkillList(Integer skillType);

    /**
     * 查询所有技能
     */
    List<SkillDict> getAllSkills();

    /**
     * 分页查询技能列表，支持按分类、技能名模糊检索
     */
    IPage<SkillDict> pageSkillList(Integer skillType, String skillNameKeyword, Long current, Long size);

    /**
     * 根据关键词搜索技能（技能名、技能描述全文检索）
     */
    List<SkillDict> searchByKeyword(String keyword);
}
