package com.caregiver.carelink.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.caregiver.carelink.dto.CaregiverSkillDTO;
import com.caregiver.carelink.entity.CaregiverSkill;
import com.caregiver.carelink.vo.CaregiverSkillVO;
import com.caregiver.carelink.vo.SkillDictWithStatusVO;

import java.util.List;

/**
 * 护工技能服务接口
 *
 * @author CareLink
 * @since 2026-01-29
 */
public interface CaregiverSkillService extends IService<CaregiverSkill> {

    /**
     * 新增技能
     */
    void addSkill(Long caregiverId, CaregiverSkillDTO dto);

    /**
     * 删除技能（根据技能字典ID删除）
     */
    void deleteSkillBySkillId(Long caregiverId, Long skillId);

    /**
     * 查询技能列表（关联查询技能字典）
     */
    List<CaregiverSkillVO> getSkillList(Long caregiverId);

    /**
     * 根据技能分类查询具备该类技能的护工ID列表（已审核通过的技能）
     *
     * @param skillType 技能分类：1-临床医疗 2-基础照料 3-康复介护 4-失智护理 5-居家安全 6-精神慰藉
     * @return 护工ID列表
     */
    List<Long> getCaregiverIdsBySkillType(Integer skillType);

    /**
     * 查询所有技能字典，并标记当前护工的申请状态
     *
     * @param caregiverId 护工ID
     * @param skillType   技能分类（可选，传null查全部）
     * @return 附带申请状态的技能字典列表
     */
    List<SkillDictWithStatusVO> getAllSkillsWithStatus(Long caregiverId, Integer skillType);
}
