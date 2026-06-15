package com.caregiver.carelink.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.caregiver.carelink.common.constant.VerifyMaterialTypeConstants;
import com.caregiver.carelink.common.exception.BusinessException;
import com.caregiver.carelink.common.result.ResultCode;
import com.caregiver.carelink.dto.LoginDTO;
import com.caregiver.carelink.entity.Admin;
import com.caregiver.carelink.entity.Caregiver;
import com.caregiver.carelink.entity.CaregiverSkill;
import com.caregiver.carelink.entity.CaregiverVerifyMaterial;
import com.caregiver.carelink.entity.SkillDict;
import com.caregiver.carelink.mapper.AdminMapper;
import com.caregiver.carelink.mapper.CaregiverMapper;
import com.caregiver.carelink.mapper.CaregiverSkillMapper;
import com.caregiver.carelink.mapper.CaregiverVerifyMaterialMapper;
import com.caregiver.carelink.service.AdminService;
import com.caregiver.carelink.service.OrderService;
import com.caregiver.carelink.service.RagDocumentService;
import com.caregiver.carelink.service.ReviewService;
import com.caregiver.carelink.service.ReviewTagService;
import com.caregiver.carelink.service.ServicePackageService;
import com.caregiver.carelink.service.SkillDictService;
import com.caregiver.carelink.service.UserService;
import com.caregiver.carelink.utils.JwtUtils;
import com.caregiver.carelink.utils.PasswordUtils;
import com.caregiver.carelink.vo.AdminStatsVO;
import com.caregiver.carelink.vo.CaregiverSettleApplyVO;
import com.caregiver.carelink.vo.CaregiverSkillApplyVO;
import com.caregiver.carelink.vo.CaregiverVerifyMaterialVO;
import com.caregiver.carelink.vo.LoginVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * 管理员服务实现
 *
 * @author CareLink
 * @since 2026-02-24
 */
@Service
public class AdminServiceImpl extends ServiceImpl<AdminMapper, Admin> implements AdminService {

    private static final Logger log = LoggerFactory.getLogger(AdminServiceImpl.class);

    /** 护工待审核状态 */
    private static final int VERIFY_STATUS_PENDING = 0;
    /** 护工审核通过 */
    private static final int VERIFY_STATUS_PASS = 1;
    /** 护工审核拒绝 */
    private static final int VERIFY_STATUS_REJECT = 2;
    /** 技能待审核 */
    private static final int AUDIT_STATUS_PENDING = 0;
    /** 技能通过 */
    private static final int AUDIT_STATUS_PASS = 1;
    /** 技能拒绝 */
    private static final int AUDIT_STATUS_REJECT = 2;

    @Resource
    private JwtUtils jwtUtils;

    @Resource
    private CaregiverMapper caregiverMapper;

    @Resource
    private CaregiverVerifyMaterialMapper caregiverVerifyMaterialMapper;

    @Resource
    private CaregiverSkillMapper caregiverSkillMapper;

    @Resource
    private SkillDictService skillDictService;

    @Resource
    private OrderService orderService;

    @Resource
    private UserService userService;

    @Resource
    private ReviewService reviewService;

    @Resource
    private ServicePackageService servicePackageService;

    @Resource
    private ReviewTagService reviewTagService;

    @Resource
    private RagDocumentService ragDocumentService;

    @Override
    public LoginVO login(LoginDTO dto) {
        log.info("管理员登录 username={}", dto.getUsername());
        LambdaQueryWrapper<Admin> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Admin::getUsername, dto.getUsername());
        Admin admin = getOne(wrapper);
        if (admin == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "用户名或密码错误");
        }
        if (!PasswordUtils.matches(dto.getPassword(), admin.getPassword())) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "用户名或密码错误");
        }
        if (admin.getStatus() != null && admin.getStatus() == 0) {
            throw new BusinessException(ResultCode.FORBIDDEN, "账号已禁用");
        }
        Map<String, Object> claims = new HashMap<>();
        claims.put("adminId", admin.getId());
        claims.put("userType", "admin");
        String token = jwtUtils.generateToken(claims);
        log.info("管理员登录成功 adminId={}", admin.getId());
        return LoginVO.builder().token(token).userType("admin").build();
    }

    @Override
    public IPage<CaregiverSettleApplyVO> pageSettleApply(String realNameKeyword, String phoneKeyword, Long current, Long size) {
        Page<Caregiver> page = new Page<>(current, size);
        LambdaQueryWrapper<Caregiver> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Caregiver::getVerifyStatus, VERIFY_STATUS_PENDING)
                .like(StringUtils.hasText(realNameKeyword), Caregiver::getRealName, realNameKeyword)
                .like(StringUtils.hasText(phoneKeyword), Caregiver::getPhone, phoneKeyword)
                .orderByDesc(Caregiver::getCreateTime);
        IPage<Caregiver> caregiverPage = caregiverMapper.selectPage(page, wrapper);
        List<CaregiverSettleApplyVO> voList = new ArrayList<>();
        for (Caregiver c : caregiverPage.getRecords()) {
            CaregiverSettleApplyVO vo = new CaregiverSettleApplyVO();
            vo.setId(c.getId());
            vo.setRealName(c.getRealName());
            vo.setPhone(c.getPhone());
            vo.setAvatar(c.getAvatar());
            vo.setGender(c.getGender());
            vo.setBirthday(c.getBirthday());
            vo.setEthnicity(c.getEthnicity());
            vo.setZodiac(c.getZodiac());
            vo.setNativePlace(c.getNativePlace());
            vo.setEducation(c.getEducation());
            vo.setWorkYears(c.getWorkYears());
            vo.setCityName(c.getCityName());
            vo.setResidentAddress(c.getResidentAddress());
            vo.setVerifyStatus(c.getVerifyStatus());
            vo.setCreateTime(c.getCreateTime());
            vo.setUpdateTime(c.getUpdateTime());
            List<CaregiverVerifyMaterial> materials = caregiverVerifyMaterialMapper.selectList(
                    new LambdaQueryWrapper<CaregiverVerifyMaterial>()
                            .eq(CaregiverVerifyMaterial::getCaregiverId, c.getId())
                            .orderByAsc(CaregiverVerifyMaterial::getMaterialType, CaregiverVerifyMaterial::getSortOrder));
            vo.setVerifyMaterials(materials.stream().map(this::toMaterialVO).collect(Collectors.toList()));
            voList.add(vo);
        }
        Page<CaregiverSettleApplyVO> result = new Page<>(caregiverPage.getCurrent(), caregiverPage.getSize(), caregiverPage.getTotal());
        result.setRecords(voList);
        return result;
    }

    @Override
    public void auditSettle(Long caregiverId, Boolean passed, String rejectReason) {
        log.info("审核护工入驻 caregiverId={}, passed={}", caregiverId, passed);
        Caregiver caregiver = caregiverMapper.selectById(caregiverId);
        if (caregiver == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "护工不存在");
        }
        if (caregiver.getVerifyStatus() == null || caregiver.getVerifyStatus() != VERIFY_STATUS_PENDING) {
            throw new BusinessException("当前状态不可审核，仅待审核的入驻申请可操作");
        }
        caregiver.setVerifyStatus(Boolean.TRUE.equals(passed) ? VERIFY_STATUS_PASS : VERIFY_STATUS_REJECT);
        caregiverMapper.updateById(caregiver);
        log.info("护工入驻审核完成 caregiverId={}, 审核结果={}", caregiverId, passed ? "通过" : "拒绝");
    }

    @Override
    public IPage<CaregiverSkillApplyVO> pageSkillApply(String caregiverNameKeyword, String caregiverPhoneKeyword,
                                                        String skillNameKeyword, Long current, Long size) {
        // 按护工姓名/手机号筛选护工ID
        List<Long> caregiverIds = null;
        if (StringUtils.hasText(caregiverNameKeyword) || StringUtils.hasText(caregiverPhoneKeyword)) {
            LambdaQueryWrapper<Caregiver> cWrapper = new LambdaQueryWrapper<>();
            cWrapper.like(StringUtils.hasText(caregiverNameKeyword), Caregiver::getRealName, caregiverNameKeyword)
                    .like(StringUtils.hasText(caregiverPhoneKeyword), Caregiver::getPhone, caregiverPhoneKeyword)
                    .select(Caregiver::getId);
            List<Caregiver> caregivers = caregiverMapper.selectList(cWrapper);
            caregiverIds = caregivers.stream().map(Caregiver::getId).collect(Collectors.toList());
            if (caregiverIds.isEmpty()) {
                Page<CaregiverSkillApplyVO> emptyPage = new Page<>(current, size, 0L);
                emptyPage.setRecords(new ArrayList<>());
                return emptyPage;
            }
        }
        // 按技能名模糊筛选技能ID
        List<Long> skillIds = null;
        if (StringUtils.hasText(skillNameKeyword)) {
            LambdaQueryWrapper<SkillDict> sWrapper = new LambdaQueryWrapper<>();
            sWrapper.like(SkillDict::getSkillName, skillNameKeyword).select(SkillDict::getId);
            List<SkillDict> skills = skillDictService.list(sWrapper);
            skillIds = skills.stream().map(SkillDict::getId).collect(Collectors.toList());
            if (skillIds == null || skillIds.isEmpty()) {
                Page<CaregiverSkillApplyVO> emptyPage = new Page<>(current, size, 0L);
                emptyPage.setRecords(new ArrayList<>());
                return emptyPage;
            }
        }

        Page<CaregiverSkill> page = new Page<>(current, size);
        LambdaQueryWrapper<CaregiverSkill> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CaregiverSkill::getAuditStatus, AUDIT_STATUS_PENDING)
                .in(caregiverIds != null && !caregiverIds.isEmpty(), CaregiverSkill::getCaregiverId, caregiverIds)
                .in(skillIds != null && !skillIds.isEmpty(), CaregiverSkill::getSkillId, skillIds)
                .orderByDesc(CaregiverSkill::getCreateTime);
        IPage<CaregiverSkill> skillPage = caregiverSkillMapper.selectPage(page, wrapper);
        List<CaregiverSkillApplyVO> voList = new ArrayList<>();
        for (CaregiverSkill cs : skillPage.getRecords()) {
            CaregiverSkillApplyVO vo = new CaregiverSkillApplyVO();
            vo.setId(cs.getId());
            vo.setCaregiverId(cs.getCaregiverId());
            vo.setSkillId(cs.getSkillId());
            vo.setCertImage(cs.getCertImage());
            vo.setAuditStatus(cs.getAuditStatus());
            vo.setCreateTime(cs.getCreateTime());
            Caregiver c = caregiverMapper.selectById(cs.getCaregiverId());
            if (c != null) {
                vo.setCaregiverName(c.getRealName());
                vo.setCaregiverPhone(c.getPhone());
            }
            SkillDict sd = skillDictService.getById(cs.getSkillId());
            if (sd != null) {
                vo.setSkillName(sd.getSkillName());
            }
            voList.add(vo);
        }
        Page<CaregiverSkillApplyVO> result = new Page<>(skillPage.getCurrent(), skillPage.getSize(), skillPage.getTotal());
        result.setRecords(voList);
        return result;
    }

    @Override
    public void auditSkillApply(Long caregiverSkillId, Boolean passed, String rejectReason) {
        log.info("审核技能申请 caregiverSkillId={}, passed={}", caregiverSkillId, passed);
        CaregiverSkill cs = caregiverSkillMapper.selectById(caregiverSkillId);
        if (cs == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "技能申请记录不存在");
        }
        if (cs.getAuditStatus() == null || cs.getAuditStatus() != AUDIT_STATUS_PENDING) {
            throw new BusinessException("当前状态不可审核，仅待审核的技能申请可操作");
        }
        cs.setAuditStatus(Boolean.TRUE.equals(passed) ? AUDIT_STATUS_PASS : AUDIT_STATUS_REJECT);
        caregiverSkillMapper.updateById(cs);
        log.info("技能审核完成 caregiverSkillId={}, caregiverId={}, skillId={}, 审核结果={}",
                caregiverSkillId, cs.getCaregiverId(), cs.getSkillId(), passed ? "通过" : "拒绝");
    }

    @Override
    public AdminStatsVO getStats() {
        CompletableFuture<Long> caregiverTotalFuture = CompletableFuture.supplyAsync(
                () -> caregiverMapper.selectCount(null));
        CompletableFuture<Long> orderTotalFuture = CompletableFuture.supplyAsync(
                () -> orderService.count());
        CompletableFuture<Long> pendingSettleFuture = CompletableFuture.supplyAsync(() -> {
            LambdaQueryWrapper<Caregiver> w = new LambdaQueryWrapper<>();
            w.eq(Caregiver::getVerifyStatus, VERIFY_STATUS_PENDING);
            return caregiverMapper.selectCount(w);
        });
        CompletableFuture<Long> pendingSkillFuture = CompletableFuture.supplyAsync(() -> {
            LambdaQueryWrapper<CaregiverSkill> w = new LambdaQueryWrapper<>();
            w.eq(CaregiverSkill::getAuditStatus, AUDIT_STATUS_PENDING);
            return caregiverSkillMapper.selectCount(w);
        });
        CompletableFuture<Long> userTotalFuture = CompletableFuture.supplyAsync(
                () -> userService.count());
        CompletableFuture<Long> reviewTotalFuture = CompletableFuture.supplyAsync(
                () -> reviewService.count());
        CompletableFuture<Long> servicePackageTotalFuture = CompletableFuture.supplyAsync(
                () -> servicePackageService.count());
        CompletableFuture<Long> skillTotalFuture = CompletableFuture.supplyAsync(
                () -> skillDictService.count());
        CompletableFuture<Long> reviewTagTotalFuture = CompletableFuture.supplyAsync(
                () -> reviewTagService.count());
        CompletableFuture<Long> ragDocumentTotalFuture = CompletableFuture.supplyAsync(
                () -> ragDocumentService.count());
        CompletableFuture<BigDecimal> todayRevenueFuture = CompletableFuture.supplyAsync(
                () -> orderService.getTodayRevenue());
        CompletableFuture<BigDecimal> totalRevenueFuture = CompletableFuture.supplyAsync(
                () -> orderService.getTotalRevenue());

        CompletableFuture.allOf(
                caregiverTotalFuture, orderTotalFuture, pendingSettleFuture, pendingSkillFuture,
                userTotalFuture, reviewTotalFuture, servicePackageTotalFuture, skillTotalFuture,
                reviewTagTotalFuture, ragDocumentTotalFuture, todayRevenueFuture, totalRevenueFuture
        ).join();

        return AdminStatsVO.builder()
                .caregiverTotal(caregiverTotalFuture.join())
                .orderTotal(orderTotalFuture.join())
                .pendingSettleTotal(pendingSettleFuture.join())
                .pendingSkillTotal(pendingSkillFuture.join())
                .userTotal(userTotalFuture.join())
                .reviewTotal(reviewTotalFuture.join())
                .servicePackageTotal(servicePackageTotalFuture.join())
                .skillTotal(skillTotalFuture.join())
                .reviewTagTotal(reviewTagTotalFuture.join())
                .ragDocumentTotal(ragDocumentTotalFuture.join())
                .todayRevenue(todayRevenueFuture.join())
                .totalRevenue(totalRevenueFuture.join())
                .build();
    }

    private CaregiverVerifyMaterialVO toMaterialVO(CaregiverVerifyMaterial m) {
        CaregiverVerifyMaterialVO vo = new CaregiverVerifyMaterialVO();
        vo.setMaterialType(m.getMaterialType());
        vo.setMaterialTypeName(materialTypeName(m.getMaterialType()));
        vo.setFileUrl(m.getFileUrl());
        vo.setSortOrder(m.getSortOrder());
        return vo;
    }

    private static String materialTypeName(Integer type) {
        if (type == null) return "";
        switch (type) {
            case VerifyMaterialTypeConstants.TYPE_ID_CARD_FRONT: return "身份证正面";
            case VerifyMaterialTypeConstants.TYPE_ID_CARD_BACK: return "身份证反面";
            case VerifyMaterialTypeConstants.TYPE_QUALIFICATION_CERT: return "护工资格证";
            case VerifyMaterialTypeConstants.TYPE_OTHER: return "其他证明材料";
            default: return "证明材料";
        }
    }
}
