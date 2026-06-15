package com.caregiver.carelink.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.caregiver.carelink.common.result.PageResult;
import com.caregiver.carelink.common.result.Result;
import com.caregiver.carelink.entity.SkillDict;
import com.caregiver.carelink.service.SkillDictService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

/**
 * 技能字典查询（公开接口，供选技能等使用；增删改请使用管理员接口 /admin/skill）
 *
 * @author CareLink
 * @since 2026-01-29
 */
@Api(tags = "技能字典管理")
@RestController
@RequestMapping("/skill")
public class SkillDictController {

    @Resource
    private SkillDictService skillDictService;

    @ApiOperation("查询所有技能（公开/选技能等用，不分页）")
    @GetMapping("/list")
    public Result<List<SkillDict>> getAllSkills(
            @ApiParam("技能分类：1-临床医疗护理 2-基础生活照料 3-康复训练与介护 4-失智专项护理 5-居家安全与应急 6-精神慰藉与社交")
            @RequestParam(required = false) Integer skillType) {
        List<SkillDict> list = skillType != null 
                ? skillDictService.getSkillList(skillType) 
                : skillDictService.getAllSkills();
        return Result.success(list);
    }

    @ApiOperation("分页查询技能列表（支持按分类、技能名检索）")
    @GetMapping("/page")
    public Result<PageResult<SkillDict>> pageSkillList(
            @ApiParam("技能分类：1-临床医疗护理 2-基础生活照料 3-康复训练与介护 4-失智专项护理 5-居家安全与应急 6-精神慰藉与社交")
            @RequestParam(required = false) Integer skillType,
            @ApiParam("技能名称（模糊）") @RequestParam(required = false) String skillName,
            @ApiParam("页码") @RequestParam(defaultValue = "1") Long current,
            @ApiParam("每页数量") @RequestParam(defaultValue = "10") Long size) {
        IPage<SkillDict> page = skillDictService.pageSkillList(skillType, skillName, current, size);
        return Result.success(PageResult.of(page));
    }

    @ApiOperation("按关键词搜索技能（技能名、技能描述全文检索）")
    @GetMapping("/search")
    public Result<List<SkillDict>> searchByKeyword(
            @ApiParam("搜索关键词（匹配技能名、技能描述）") @RequestParam(required = false) String keyword) {
        List<SkillDict> list = skillDictService.searchByKeyword(keyword != null ? keyword : "");
        return Result.success(list);
    }
}
