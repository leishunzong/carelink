package com.caregiver.carelink.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.caregiver.carelink.common.result.PageResult;
import com.caregiver.carelink.common.result.Result;
import com.caregiver.carelink.dto.ServicePackageDTO;
import com.caregiver.carelink.service.ServicePackageService;
import com.caregiver.carelink.vo.ServicePackageVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

/**
 * 服务包管理控制器
 *
 * @author CareLink
 * @since 2026-02-11
 */
@Api(tags = "服务包管理")
@RestController
@RequestMapping("/package")
public class ServicePackageController {

    @Resource
    private ServicePackageService servicePackageService;

    @ApiOperation("分页查询上架服务包列表（用户/护工通用，仅返回上架服务包）")
    @GetMapping("/page")
    public Result<PageResult<ServicePackageVO>> page(
            @ApiParam("服务类型：1-居家陪护 2-医院陪护 3-周期护理 4-家政服务 5-陪诊服务 6-母婴护理") @RequestParam(required = false) Integer category,
            @ApiParam("页码") @RequestParam(defaultValue = "1") Long current,
            @ApiParam("每页数量") @RequestParam(defaultValue = "10") Long size) {
        IPage<ServicePackageVO> page = servicePackageService.pageList(category, 1, current, size);
        return Result.success(PageResult.of(page));
    }

    @ApiOperation("关键词搜索服务包（仅上架，按名称与描述全文检索）")
    @GetMapping("/search")
    public Result<PageResult<ServicePackageVO>> search(
            @ApiParam("搜索关键词（为空则按分类分页列出）") @RequestParam(required = false) String keyword,
            @ApiParam("服务类型：1-居家陪护 2-医院陪护 3-周期护理 4-家政服务 5-陪诊服务 6-母婴护理") @RequestParam(required = false) Integer category,
            @ApiParam("页码") @RequestParam(defaultValue = "1") Long current,
            @ApiParam("每页数量") @RequestParam(defaultValue = "10") Long size) {
        IPage<ServicePackageVO> page = servicePackageService.search(keyword, category, current, size);
        return Result.success(PageResult.of(page));
    }

    @ApiOperation("热门/推荐搜索关键词（按销量取上架服务包名称，供搜索框推荐）")
    @GetMapping("/hot-keywords")
    public Result<List<String>> hotKeywords(
            @ApiParam("返回条数，默认10，最大20") @RequestParam(defaultValue = "10") Integer limit) {
        List<String> keywords = servicePackageService.getHotKeywords(limit);
        return Result.success(keywords);
    }

    @ApiOperation("根据ID查询服务包详情")
    @GetMapping("/{id}")
    public Result<ServicePackageVO> getById(
            @ApiParam(value = "服务包ID", required = true) @PathVariable Long id) {
        ServicePackageVO vo = servicePackageService.getDetailById(id);
        return Result.success(vo);
    }

    // 新增 / 修改 / 删除 服务包改由管理员端 AdminController 统一管理
}
