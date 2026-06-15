package com.caregiver.carelink.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.caregiver.carelink.common.context.CaregiverContextHolder;
import com.caregiver.carelink.common.result.PageResult;
import com.caregiver.carelink.common.result.Result;
import com.caregiver.carelink.dto.*;
import com.caregiver.carelink.service.CaregiverService;
import com.caregiver.carelink.service.CaregiverServiceConfigService;
import com.caregiver.carelink.service.CaregiverSkillService;
import com.caregiver.carelink.service.ServicePackageService;
import com.caregiver.carelink.vo.CaregiverInfoVO;
import com.caregiver.carelink.vo.CaregiverDetailVO;
import com.caregiver.carelink.vo.CaregiverSkillVO;
import com.caregiver.carelink.vo.LoginVO;
import com.caregiver.carelink.vo.CaregiverMyPackageVO;
import com.caregiver.carelink.vo.ServicePackageVO;
import com.caregiver.carelink.vo.ServicePackageWithStatusVO;
import com.caregiver.carelink.vo.SkillDictWithStatusVO;
import com.caregiver.carelink.vo.NearbyCaregiverVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.List;

/**
 * 护工控制器
 *
 * @author CareLink
 * @since 2026-01-29
 */
@Api(tags = "护工管理")
@RestController
@RequestMapping("/caregiver")
public class CaregiverController {

    @Resource
    private CaregiverService caregiverService;

    @Resource
    private CaregiverSkillService caregiverSkillService;

    @Resource
    private CaregiverServiceConfigService caregiverServiceConfigService;

    @Resource
    private ServicePackageService servicePackageService;

    @ApiOperation("护工注册")
    @PostMapping("/register")
    public Result<Void> register(@Validated @RequestBody CaregiverRegisterDTO dto) {
        caregiverService.register(dto);
        return Result.success("注册成功，请先入驻提交审核");
    }

    @ApiOperation("护工登录")
    @PostMapping("/login")
    public Result<LoginVO> login(@Validated @RequestBody LoginDTO dto) {
        LoginVO loginVO = caregiverService.login(dto);
        return Result.success(loginVO);
    }

    // ==================== 护工基础信息管理 ====================

    @ApiOperation("护工入驻（补齐基本信息并提交审核材料；待审核或审核被拒可提交，被拒后可重新提交再次审核）")
    @PostMapping("/settle")
    public Result<Void> settle(@Validated @RequestBody CaregiverSettleDTO dto) {
        Long caregiverId = CaregiverContextHolder.getCaregiverId();
        caregiverService.settle(caregiverId, dto);
        return Result.success("入驻信息已提交，请等待审核");
    }

    @ApiOperation("获取我的信息")
    @GetMapping("/info")
    public Result<CaregiverInfoVO> getMyInfo() {
        Long caregiverId = CaregiverContextHolder.getCaregiverId();
        CaregiverInfoVO caregiverInfo = caregiverService.getCaregiverInfo(caregiverId);
        return Result.success(caregiverInfo);
    }

    @ApiOperation("修改我的信息")
    @PutMapping("/info")
    public Result<Void> updateMyInfo(@Validated @RequestBody CaregiverUpdateDTO dto) {
        Long caregiverId = CaregiverContextHolder.getCaregiverId();
        caregiverService.updateCaregiverInfo(caregiverId, dto);
        return Result.success("修改成功");
    }

    @ApiOperation("更新我的接单位置")
    @PostMapping("/location")
    public Result<Void> updateLocation(
            @ApiParam(value = "经度", required = true) @RequestParam BigDecimal longitude,
            @ApiParam(value = "纬度", required = true) @RequestParam BigDecimal latitude) {
        Long caregiverId = CaregiverContextHolder.getCaregiverId();
        caregiverService.updateLocation(caregiverId, longitude, latitude);
        return Result.success("位置更新成功");
    }

    @ApiOperation("切换工作状态")
    @PostMapping("/work-state")
    public Result<Void> updateWorkState(
            @ApiParam(value = "工作状态: 1接单中 3休息中", required = true) @RequestParam Integer workState) {
        Long caregiverId = CaregiverContextHolder.getCaregiverId();
        caregiverService.updateWorkState(caregiverId, workState);
        return Result.success("状态切换成功");
    }

    @ApiOperation("修改密码")
    @PutMapping("/password")
    public Result<Void> updatePassword(@Validated @RequestBody PasswordUpdateDTO dto) {
        Long caregiverId = CaregiverContextHolder.getCaregiverId();
        caregiverService.updatePassword(caregiverId, dto.getOldPassword(), dto.getNewPassword());
        return Result.success("密码修改成功");
    }

    // ==================== 护工技能管理 ====================

    @ApiOperation("新增技能")
    @PostMapping("/skill")
    public Result<Void> addSkill(@Validated @RequestBody CaregiverSkillDTO dto) {
        Long caregiverId = CaregiverContextHolder.getCaregiverId();
        caregiverSkillService.addSkill(caregiverId, dto);
        return Result.success("添加成功");
    }

    @ApiOperation("删除技能")
    @DeleteMapping("/skill/{skillId}")
    public Result<Void> deleteSkill(
            @ApiParam(value = "技能字典ID", required = true) @PathVariable Long skillId) {
        Long caregiverId = CaregiverContextHolder.getCaregiverId();
        caregiverSkillService.deleteSkillBySkillId(caregiverId, skillId);
        return Result.success("删除成功");
    }

    @ApiOperation("查询我的技能列表")
    @GetMapping("/skill/list")
    public Result<List<CaregiverSkillVO>> getMySkillList() {
        Long caregiverId = CaregiverContextHolder.getCaregiverId();
        List<CaregiverSkillVO> list = caregiverSkillService.getSkillList(caregiverId);
        return Result.success(list);
    }

    @ApiOperation("查询所有技能（附带我的申请状态，用于护工端技能列表区分已申请/未申请）")
    @GetMapping("/skill/all")
    public Result<List<SkillDictWithStatusVO>> getAllSkillsWithStatus(
            @ApiParam("技能分类：1-临床医疗护理 2-基础生活照料 3-康复训练与介护 4-失智专项护理 5-居家安全与应急 6-精神慰藉与社交")
            @RequestParam(required = false) Integer skillType) {
        Long caregiverId = CaregiverContextHolder.getCaregiverId();
        List<SkillDictWithStatusVO> list = caregiverSkillService.getAllSkillsWithStatus(caregiverId, skillType);
        return Result.success(list);
    }

    // ==================== 护工服务包管理 ====================

    @ApiOperation("查询我开通的服务包列表（基本信息+准入时间）")
    @GetMapping("/package/my")
    public Result<List<CaregiverMyPackageVO>> getMyPackages() {
        Long caregiverId = CaregiverContextHolder.getCaregiverId();
        List<CaregiverMyPackageVO> list = servicePackageService.getCaregiverMyPackages(caregiverId);
        return Result.success(list);
    }

    @ApiOperation("分页查询可开通的服务包（仅上架，按类型筛选，供护工选择开通）")
    @GetMapping("/package/list")
    public Result<PageResult<ServicePackageVO>> getServicePackageList(
            @ApiParam("服务类型：1-居家陪护 2-医院陪护 3-周期护理 4-家政服务 5-陪诊服务 6-母婴护理") @RequestParam(required = false) Integer category,
            @ApiParam("页码") @RequestParam(defaultValue = "1") Long current,
            @ApiParam("每页数量") @RequestParam(defaultValue = "10") Long size) {
        IPage<ServicePackageVO> page = servicePackageService.pageList(category, 1, current, size);
        return Result.success(PageResult.of(page));
    }

    @ApiOperation("分页查询上架服务包（附带我的开通状态，用于护工端服务包列表区分已开通/未开通）")
    @GetMapping("/package/available")
    public Result<PageResult<ServicePackageWithStatusVO>> getAvailablePackagesWithStatus(
            @ApiParam("服务类型：1-居家陪护 2-医院陪护 3-周期护理 4-家政服务 5-陪诊服务 6-母婴护理") @RequestParam(required = false) Integer category,
            @ApiParam("页码") @RequestParam(defaultValue = "1") Long current,
            @ApiParam("每页数量") @RequestParam(defaultValue = "10") Long size) {
        Long caregiverId = CaregiverContextHolder.getCaregiverId();
        IPage<ServicePackageWithStatusVO> page = servicePackageService.pageListWithStatus(caregiverId, category, current, size);
        return Result.success(PageResult.of(page));
    }

    @ApiOperation("添加服务包（护工开通可接单的服务包，用于系统匹配）")
    @PostMapping("/package")
    public Result<Void> addServicePackage(@Validated @RequestBody CaregiverPackageBindDTO dto) {
        Long caregiverId = CaregiverContextHolder.getCaregiverId();
        caregiverServiceConfigService.addAssociation(caregiverId, dto.getPackageId());
        return Result.success("添加成功");
    }

    @ApiOperation("取消服务包（护工取消该服务包准入）")
    @DeleteMapping("/package/{packageId}")
    public Result<Void> removeServicePackage(
            @ApiParam(value = "服务包ID", required = true) @PathVariable Long packageId) {
        Long caregiverId = CaregiverContextHolder.getCaregiverId();
        caregiverServiceConfigService.removeAssociation(caregiverId, packageId);
        return Result.success("已取消");
    }

    // ==================== 用户端查询护工 ====================

    @ApiOperation("查询护工详情聚合信息（基础信息 + 技能 + 服务包 + 统计；内部并行查询。评价请用评价分页接口）")
    @GetMapping("/public/{caregiverId}/detail")
    public Result<CaregiverDetailVO> getCaregiverDetail(
            @ApiParam(value = "护工ID", required = true) @PathVariable Long caregiverId) {
        CaregiverDetailVO vo = caregiverService.getCaregiverDetailAggregation(caregiverId);
        return Result.success(vo);
    }

    @ApiOperation("查询护工基础信息（用户查看护工详情）")
    @GetMapping("/public/{caregiverId}")
    public Result<CaregiverInfoVO> getCaregiverBasicInfo(
            @ApiParam(value = "护工ID", required = true) @PathVariable Long caregiverId) {
        CaregiverInfoVO info = caregiverService.getCaregiverInfo(caregiverId);
        return Result.success(info);
    }

    @ApiOperation("查询护工技能列表（用户查看护工详情）")
    @GetMapping("/public/{caregiverId}/skills")
    public Result<List<CaregiverSkillVO>> getCaregiverSkills(
            @ApiParam(value = "护工ID", required = true) @PathVariable Long caregiverId) {
        List<CaregiverSkillVO> list = caregiverSkillService.getSkillList(caregiverId);
        return Result.success(list);
    }

    @ApiOperation("查询护工可提供的服务包列表（用户查看护工详情）")
    @GetMapping("/public/{caregiverId}/packages")
    public Result<List<ServicePackageVO>> getCaregiverPackages(
            @ApiParam(value = "护工ID", required = true) @PathVariable Long caregiverId) {
        List<ServicePackageVO> list = servicePackageService.getCaregiverPackages(caregiverId);
        return Result.success(list);
    }

    @ApiOperation("分页搜索护工列表（用户端）")
    @PostMapping("/search")
    public Result<Page<CaregiverInfoVO>> searchCaregivers(@Validated @RequestBody CaregiverSearchDTO dto) {
        Page<CaregiverInfoVO> page = caregiverService.searchCaregivers(dto);
        return Result.success(page);
    }

    @ApiOperation("根据当前位置查询附近护工列表（用户端）")
    @GetMapping("/nearby")
    public Result<List<NearbyCaregiverVO>> getNearbyCaregivers(
            @ApiParam(value = "城市编码", required = true) @RequestParam String cityCode,
            @ApiParam(value = "当前经度", required = true) @RequestParam BigDecimal longitude,
            @ApiParam(value = "当前纬度", required = true) @RequestParam BigDecimal latitude,
            @ApiParam(value = "返回数量上限（默认20，最大50）") @RequestParam(required = false) Integer limit) {
        List<NearbyCaregiverVO> list = caregiverService.findNearbyCaregivers(cityCode, longitude, latitude, limit);
        return Result.success(list);
    }
}

