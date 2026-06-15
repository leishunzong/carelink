package com.caregiver.carelink.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.caregiver.carelink.dto.CaregiverRegisterDTO;
import com.caregiver.carelink.dto.CaregiverSearchDTO;
import com.caregiver.carelink.dto.CaregiverSettleDTO;
import com.caregiver.carelink.dto.CaregiverUpdateDTO;
import com.caregiver.carelink.dto.LoginDTO;
import com.caregiver.carelink.entity.Caregiver;
import com.caregiver.carelink.vo.CaregiverDetailVO;
import com.caregiver.carelink.vo.CaregiverInfoVO;
import com.caregiver.carelink.vo.LoginVO;
import com.caregiver.carelink.vo.NearbyCaregiverVO;

import java.math.BigDecimal;

/**
 * 护工服务接口
 *
 * @author CareLink
 * @since 2026-01-29
 */
public interface CaregiverService extends IService<Caregiver> {

    /**
     * 护工注册
     */
    void register(CaregiverRegisterDTO dto);

    /**
     * 护工登录
     */
    LoginVO login(LoginDTO dto);

    /**
     * 获取护工信息
     */
    CaregiverInfoVO getCaregiverInfo(Long caregiverId);

    /**
     * 护工详情聚合（基础信息 + 技能 + 服务包 + 统计），内部并行查询，评价请用单独分页接口
     */
    CaregiverDetailVO getCaregiverDetailAggregation(Long caregiverId);

    /**
     * 护工入驻：补齐护工表信息并提交审核材料（身份证、资格证、其他证明）。待审核(0)或审核被拒(2)可调用；被拒后重新提交会置为待审核再次审核。
     */
    void settle(Long caregiverId, CaregiverSettleDTO dto);

    /**
     * 更新护工信息
     */
    void updateCaregiverInfo(Long caregiverId, CaregiverUpdateDTO dto);

    /**
     * 更新护工位置
     */
    void updateLocation(Long caregiverId, BigDecimal longitude, BigDecimal latitude);

    /**
     * 切换工作状态
     */
    void updateWorkState(Long caregiverId, Integer workState);

    /**
     * 修改密码
     */
    void updatePassword(Long caregiverId, String oldPassword, String newPassword);

    /**
     * 分页搜索护工列表（用户端）
     */
    Page<CaregiverInfoVO> searchCaregivers(CaregiverSearchDTO dto);

    /**
     * 根据经纬度搜索附近护工列表（用户端，服务端按固定半径搜索并按距离排序）
     */
    java.util.List<NearbyCaregiverVO> findNearbyCaregivers(String cityCode, BigDecimal longitude, BigDecimal latitude,
                                                           Integer limit);

    /**
     * 管理员分页查询护工列表，支持护工名、性别、手机号、年龄、学历、从业年限、服务城市名、工作状态检索，按创建时间倒序
     */
    IPage<CaregiverInfoVO> pageCaregiversForAdmin(String realNameKeyword, String phoneKeyword, Integer gender,
                                                  Integer minAge, Integer maxAge, String education, Integer workYears,
                                                  String cityNameKeyword, Integer workState, Long current, Long size);
}
