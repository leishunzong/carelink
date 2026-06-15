package com.caregiver.carelink.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.caregiver.carelink.common.result.PageResult;
import com.caregiver.carelink.common.result.Result;
import com.caregiver.carelink.dto.LoginDTO;
import com.caregiver.carelink.dto.ReviewTagDTO;
import com.caregiver.carelink.dto.SettleAuditDTO;
import com.caregiver.carelink.dto.SkillApplyAuditDTO;
import com.caregiver.carelink.dto.SkillDictDTO;
import com.caregiver.carelink.dto.ServicePackageDTO;
import com.caregiver.carelink.entity.RagDocument;
import com.caregiver.carelink.service.AdminService;
import com.caregiver.carelink.service.CaregiverService;
import com.caregiver.carelink.service.OrderService;
import com.caregiver.carelink.service.RagDocumentService;
import com.caregiver.carelink.service.ReviewService;
import com.caregiver.carelink.service.ReviewTagService;
import com.caregiver.carelink.service.SkillDictService;
import com.caregiver.carelink.service.ServicePackageService;
import com.caregiver.carelink.vo.AdminStatsVO;
import com.caregiver.carelink.vo.CaregiverInfoVO;
import com.caregiver.carelink.vo.CaregiverSettleApplyVO;
import com.caregiver.carelink.vo.CaregiverSkillApplyVO;
import com.caregiver.carelink.vo.LoginVO;
import com.caregiver.carelink.vo.AdminOrderListItemVO;
import com.caregiver.carelink.vo.ReviewVO;
import com.caregiver.carelink.vo.RagDocumentVO;
import com.caregiver.carelink.vo.ServicePackageVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 管理员端统一入口：登录、护工入驻/技能审核、评价标签、技能字典、RAG 知识库
 *
 * @author CareLink
 * @since 2026-02-24
 */
@Api(tags = "管理员")
@RestController
@RequestMapping("/admin")
public class AdminController {

    @Resource
    private AdminService adminService;

    @Resource
    private RagDocumentService ragDocumentService;

    @Resource
    private ReviewTagService reviewTagService;

    @Resource
    private SkillDictService skillDictService;

    @Resource
    private ServicePackageService servicePackageService;

    @Resource
    private ReviewService reviewService;

    @Resource
    private CaregiverService caregiverService;

    @Resource
    private OrderService orderService;

    @ApiOperation("管理员登录")
    @PostMapping("/login")
    public Result<LoginVO> login(@Validated @RequestBody LoginDTO dto) {
        LoginVO vo = adminService.login(dto);
        return Result.success(vo);
    }

    @ApiOperation("查询统计数据（护工、订单、待审核、用户、评价、服务包、技能、评价标签、知识库总数），异步并行查询")
    @GetMapping("/stats")
    public Result<AdminStatsVO> getStats() {
        AdminStatsVO vo = adminService.getStats();
        return Result.success(vo);
    }

    @ApiOperation("分页查询护工入驻申请列表（待审核），支持按姓名、手机号检索")
    @GetMapping("/caregiver/settle-list")
    public Result<PageResult<CaregiverSettleApplyVO>> getSettleApplyList(
            @ApiParam("护工姓名（模糊）") @RequestParam(required = false) String realName,
            @ApiParam("护工手机号（模糊）") @RequestParam(required = false) String phone,
            @ApiParam("页码") @RequestParam(defaultValue = "1") Long current,
            @ApiParam("每页数量") @RequestParam(defaultValue = "10") Long size) {
        IPage<CaregiverSettleApplyVO> page = adminService.pageSettleApply(realName, phone, current, size);
        return Result.success(PageResult.of(page));
    }

    @ApiOperation("护工入驻申请审核（通过/拒绝）")
    @PostMapping("/caregiver/settle/audit")
    public Result<Void> auditSettle(@Validated @RequestBody SettleAuditDTO dto) {
        adminService.auditSettle(dto.getCaregiverId(), dto.getPassed(), dto.getRejectReason());
        return Result.success(dto.getPassed() ? "已通过" : "已拒绝");
    }

    @ApiOperation("分页查询护工技能申请列表（待审核），支持按护工姓名、手机号、技能名检索")
    @GetMapping("/caregiver/skill-apply-list")
    public Result<PageResult<CaregiverSkillApplyVO>> getSkillApplyList(
            @ApiParam("护工姓名（模糊）") @RequestParam(required = false) String caregiverName,
            @ApiParam("护工手机号（模糊）") @RequestParam(required = false) String caregiverPhone,
            @ApiParam("技能名称（模糊）") @RequestParam(required = false) String skillName,
            @ApiParam("页码") @RequestParam(defaultValue = "1") Long current,
            @ApiParam("每页数量") @RequestParam(defaultValue = "10") Long size) {
        IPage<CaregiverSkillApplyVO> page = adminService.pageSkillApply(caregiverName, caregiverPhone, skillName, current, size);
        return Result.success(PageResult.of(page));
    }

    @ApiOperation("护工技能申请审核（通过/拒绝）")
    @PostMapping("/caregiver/skill-apply/audit")
    public Result<Void> auditSkillApply(@Validated @RequestBody SkillApplyAuditDTO dto) {
        adminService.auditSkillApply(dto.getCaregiverSkillId(), dto.getPassed(), dto.getRejectReason());
        return Result.success(dto.getPassed() ? "已通过" : "已拒绝");
    }

    // ---------- 分页查询（管理员） ----------

    @ApiOperation("分页查询评价列表（支持用户昵称、护工名、订单号检索，按创建时间倒序）")
    @GetMapping("/review/page")
    public Result<PageResult<ReviewVO>> pageReviewList(
            @ApiParam("用户昵称（模糊）") @RequestParam(required = false) String nickname,
            @ApiParam("护工名（模糊）") @RequestParam(required = false) String caregiverName,
            @ApiParam("订单号（模糊）") @RequestParam(required = false) String orderNo,
            @ApiParam("页码") @RequestParam(defaultValue = "1") Long current,
            @ApiParam("每页数量") @RequestParam(defaultValue = "10") Long size) {
        IPage<ReviewVO> page = reviewService.pageReviewsForAdmin(nickname, caregiverName, orderNo, current, size);
        return Result.success(PageResult.of(page));
    }

    @ApiOperation("分页查询护工列表（支持护工名、性别、手机号、年龄、学历、从业年限、服务城市名、工作状态检索，按创建时间倒序）")
    @GetMapping("/caregiver/page")
    public Result<PageResult<CaregiverInfoVO>> pageCaregiverList(
            @ApiParam("护工名（模糊）") @RequestParam(required = false) String realName,
            @ApiParam("手机号（模糊）") @RequestParam(required = false) String phone,
            @ApiParam("性别：1男 2女") @RequestParam(required = false) Integer gender,
            @ApiParam("最小年龄") @RequestParam(required = false) Integer minAge,
            @ApiParam("最大年龄") @RequestParam(required = false) Integer maxAge,
            @ApiParam("学历（模糊）") @RequestParam(required = false) String education,
            @ApiParam("从业年限") @RequestParam(required = false) Integer workYears,
            @ApiParam("服务城市名（模糊）") @RequestParam(required = false) String cityName,
            @ApiParam("工作状态：1接单中 2服务中 3休息中") @RequestParam(required = false) Integer workState,
            @ApiParam("页码") @RequestParam(defaultValue = "1") Long current,
            @ApiParam("每页数量") @RequestParam(defaultValue = "10") Long size) {
        IPage<CaregiverInfoVO> page = caregiverService.pageCaregiversForAdmin(
                realName, phone, gender, minAge, maxAge, education, workYears, cityName, workState, current, size);
        return Result.success(PageResult.of(page));
    }

    @ApiOperation("分页查询订单列表（支持订单号、订单类型、订单状态、护工名、联系人姓名、城市名检索，按创建时间倒序）")
    @GetMapping("/order/page")
    public Result<PageResult<AdminOrderListItemVO>> pageOrderList(
            @ApiParam("订单号（模糊）") @RequestParam(required = false) String orderNo,
            @ApiParam("订单类型：1系统匹配 2定向预约") @RequestParam(required = false) Integer orderType,
            @ApiParam("订单状态：1-8") @RequestParam(required = false) Integer status,
            @ApiParam("护工名（模糊）") @RequestParam(required = false) String caregiverName,
            @ApiParam("联系人姓名（模糊）") @RequestParam(required = false) String contactName,
            @ApiParam("城市名（模糊）") @RequestParam(required = false) String cityName,
            @ApiParam("页码") @RequestParam(defaultValue = "1") Long current,
            @ApiParam("每页数量") @RequestParam(defaultValue = "10") Long size) {
        IPage<AdminOrderListItemVO> page = orderService.pageOrdersForAdmin(
                orderNo, orderType, status, caregiverName, contactName, cityName, current, size);
        return Result.success(PageResult.of(page));
    }

    // ---------- 评价标签（管理员） ----------
    @ApiOperation("新增评价标签（管理员）")
    @PostMapping("/tag")
    public Result<Void> addTag(@Validated @RequestBody ReviewTagDTO dto) {
        reviewTagService.addTag(dto);
        return Result.success("添加成功");
    }

    @ApiOperation("修改评价标签（管理员）")
    @PutMapping("/tag/{tagId}")
    public Result<Void> updateTag(
            @ApiParam(value = "标签ID", required = true) @PathVariable Long tagId,
            @Validated @RequestBody ReviewTagDTO dto) {
        reviewTagService.updateTag(tagId, dto);
        return Result.success("修改成功");
    }

    // ---------- 技能字典（管理员） ----------
    @ApiOperation("新增技能（管理员）")
    @PostMapping("/skill")
    public Result<Void> addSkill(@Validated @RequestBody SkillDictDTO dto) {
        skillDictService.addSkill(dto);
        return Result.success("添加成功");
    }

    @ApiOperation("修改技能（管理员）")
    @PutMapping("/skill/{skillId}")
    public Result<Void> updateSkill(
            @ApiParam(value = "技能ID", required = true) @PathVariable Long skillId,
            @Validated @RequestBody SkillDictDTO dto) {
        skillDictService.updateSkill(skillId, dto);
        return Result.success("修改成功");
    }

    @ApiOperation("删除技能（管理员）")
    @DeleteMapping("/skill/{skillId}")
    public Result<Void> deleteSkill(
            @ApiParam(value = "技能ID", required = true) @PathVariable Long skillId) {
        skillDictService.deleteSkill(skillId);
        return Result.success("删除成功");
    }

    // ---------- 服务包管理（管理员） ----------

    @ApiOperation("分页查询服务包列表（管理员，可按类型和状态筛选）")
    @GetMapping("/package/page")
    public Result<PageResult<ServicePackageVO>> pageServicePackage(
            @ApiParam("服务类型：1-居家陪护 2-医院陪护 3-周期护理 4-家政服务 5-陪诊服务 6-母婴护理") @RequestParam(required = false) Integer category,
            @ApiParam("状态：1-上架 0-下架") @RequestParam(required = false) Integer status,
            @ApiParam("页码") @RequestParam(defaultValue = "1") Long current,
            @ApiParam("每页数量") @RequestParam(defaultValue = "10") Long size) {
        IPage<ServicePackageVO> page = servicePackageService.pageList(category, status, current, size);
        return Result.success(PageResult.of(page));
    }

    @ApiOperation("新增服务包（管理员）")
    @PostMapping("/package")
    public Result<Void> addServicePackage(@Validated @RequestBody ServicePackageDTO dto) {
        servicePackageService.addPackage(dto);
        return Result.success("添加成功");
    }

    @ApiOperation("修改服务包（管理员）")
    @PutMapping("/package/{id}")
    public Result<Void> updateServicePackage(
            @ApiParam(value = "服务包ID", required = true) @PathVariable Long id,
            @Validated @RequestBody ServicePackageDTO dto) {
        servicePackageService.updatePackageById(id, dto);
        return Result.success("修改成功");
    }

    @ApiOperation("删除服务包（管理员）")
    @DeleteMapping("/package/{id}")
    public Result<Void> deleteServicePackage(
            @ApiParam(value = "服务包ID", required = true) @PathVariable Long id) {
        servicePackageService.removePackageById(id);
        return Result.success("删除成功");
    }

    @ApiOperation("上架服务包（管理员）")
    @PutMapping("/package/{id}/on-shelf")
    public Result<Void> onShelfServicePackage(
            @ApiParam(value = "服务包ID", required = true) @PathVariable Long id) {
        servicePackageService.onShelf(id);
        return Result.success("已上架");
    }

    @ApiOperation("下架服务包（管理员）")
    @PutMapping("/package/{id}/off-shelf")
    public Result<Void> offShelfServicePackage(
            @ApiParam(value = "服务包ID", required = true) @PathVariable Long id) {
        servicePackageService.offShelf(id);
        return Result.success("已下架");
    }

    // ---------- RAG 知识库（管理员） ----------
    @ApiOperation("上传知识库文档（txt/md，内容将落库并加入向量库）")
    @PostMapping("/rag/document")
    public Result<RagDocumentVO> uploadRagDocument(
            @ApiParam("文件（.txt 或 .md）") @RequestParam("file") MultipartFile file,
            @ApiParam("文档标题（不传则用文件名）") @RequestParam(required = false) String title) {
        String fileName = file.getOriginalFilename();
        if (!StringUtils.hasText(fileName) || (!fileName.toLowerCase().endsWith(".txt") && !fileName.toLowerCase().endsWith(".md"))) {
            return Result.fail("仅支持 .txt 或 .md 文件");
        }
        String content;
        try {
            content = new String(file.getBytes(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            return Result.fail("文件读取失败");
        }
        if (!StringUtils.hasText(content.trim())) {
            return Result.fail("文件内容不能为空");
        }
        RagDocument doc = ragDocumentService.addDocument(title, fileName, content);
        return Result.success(ragDocumentToVO(doc));
    }

    @ApiOperation("新增知识库文档（直接提交标题和正文，不入文件）")
    @PostMapping("/rag/document/text")
    public Result<RagDocumentVO> addRagDocumentText(
            @ApiParam(value = "文档标题", required = true) @RequestParam String title,
            @ApiParam(value = "正文内容", required = true) @RequestParam String content) {
        RagDocument doc = ragDocumentService.addDocument(title, null, content);
        return Result.success(ragDocumentToVO(doc));
    }

    @ApiOperation("知识库文档列表（按创建时间倒序）")
    @GetMapping("/rag/documents")
    public Result<List<RagDocumentVO>> listRagDocuments() {
        List<RagDocument> list = ragDocumentService.listDocuments();
        List<RagDocumentVO> voList = list.stream().map(this::ragDocumentToVO).collect(Collectors.toList());
        return Result.success(voList);
    }

    @ApiOperation("删除知识库文档（仅移除该文档向量，不重建整个库）")
    @DeleteMapping("/rag/document/{id}")
    public Result<Void> deleteRagDocument(
            @ApiParam(value = "文档ID", required = true) @PathVariable Long id) {
        ragDocumentService.deleteDocumentAndRebuild(id);
        return Result.success("已删除");
    }

    private RagDocumentVO ragDocumentToVO(RagDocument doc) {
        RagDocumentVO vo = new RagDocumentVO();
        vo.setId(doc.getId());
        vo.setTitle(doc.getTitle());
        vo.setFileName(doc.getFileName());
        vo.setContent(doc.getContent());
        vo.setCreateTime(doc.getCreateTime());
        vo.setUpdateTime(doc.getUpdateTime());
        return vo;
    }
}
