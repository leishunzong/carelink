package com.caregiver.carelink.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.caregiver.carelink.dto.LoginDTO;
import com.caregiver.carelink.entity.Admin;
import com.caregiver.carelink.vo.AdminStatsVO;
import com.caregiver.carelink.vo.CaregiverSettleApplyVO;
import com.caregiver.carelink.vo.CaregiverSkillApplyVO;
import com.caregiver.carelink.vo.LoginVO;

/**
 * 管理员服务（登录 + 护工入驻/技能审核）
 *
 * @author CareLink
 * @since 2026-02-24
 */
public interface AdminService extends IService<Admin> {

    /**
     * 管理员登录，返回 token（userType=admin）
     */
    LoginVO login(LoginDTO dto);

    /**
     * 分页查询护工入驻申请列表（verify_status=0 待审核），支持按姓名、手机号模糊检索
     */
    IPage<CaregiverSettleApplyVO> pageSettleApply(String realNameKeyword, String phoneKeyword, Long current, Long size);

    /**
     * 护工入驻申请审核：通过(1)或拒绝(2)
     *
     * @param caregiverId 护工ID
     * @param passed      是否通过
     * @param rejectReason 拒绝原因（拒绝时建议填写）
     */
    void auditSettle(Long caregiverId, Boolean passed, String rejectReason);

    /**
     * 分页查询护工技能申请列表（audit_status=0 待审核），支持按护工姓名、手机号、技能名模糊检索
     */
    IPage<CaregiverSkillApplyVO> pageSkillApply(String caregiverNameKeyword, String caregiverPhoneKeyword,
                                                String skillNameKeyword, Long current, Long size);

    /**
     * 护工技能申请审核：通过(1)或拒绝(2)
     *
     * @param caregiverSkillId 护工技能记录ID
     * @param passed           是否通过
     * @param rejectReason     拒绝原因（可选）
     */
    void auditSkillApply(Long caregiverSkillId, Boolean passed, String rejectReason);

    /**
     * 查询管理端统计数据（护工、订单、待审核、用户、评价、服务包、技能、评价标签、知识库总数），使用异步编排并行查询
     */
    AdminStatsVO getStats();
}
