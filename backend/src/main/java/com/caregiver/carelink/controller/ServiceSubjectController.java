package com.caregiver.carelink.controller;

import com.caregiver.carelink.common.context.UserContextHolder;
import com.caregiver.carelink.common.result.Result;
import com.caregiver.carelink.dto.ServiceSubjectDTO;
import com.caregiver.carelink.entity.ServiceSubject;
import com.caregiver.carelink.service.ServiceSubjectService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

/**
 * 服务对象控制器
 *
 * @author CareLink
 * @since 2026-01-29
 */
@Api(tags = "服务对象管理")
@RestController
@RequestMapping("/user/subject")
public class ServiceSubjectController {

    @Resource
    private ServiceSubjectService serviceSubjectService;

    @ApiOperation("新增服务对象")
    @PostMapping
    public Result<Void> addSubject(@Validated @RequestBody ServiceSubjectDTO dto) {
        Long userId = UserContextHolder.getUserId();
        serviceSubjectService.addSubject(userId, dto);
        return Result.success("添加成功");
    }

    @ApiOperation("修改服务对象")
    @PutMapping("/{subjectId}")
    public Result<Void> updateSubject(
            @ApiParam(value = "服务对象ID", required = true) @PathVariable Long subjectId,
            @Validated @RequestBody ServiceSubjectDTO dto) {
        Long userId = UserContextHolder.getUserId();
        serviceSubjectService.updateSubject(userId, subjectId, dto);
        return Result.success("修改成功");
    }

    @ApiOperation("删除服务对象")
    @DeleteMapping("/{subjectId}")
    public Result<Void> deleteSubject(
            @ApiParam(value = "服务对象ID", required = true) @PathVariable Long subjectId) {
        Long userId = UserContextHolder.getUserId();
        serviceSubjectService.deleteSubject(userId, subjectId);
        return Result.success("删除成功");
    }

    @ApiOperation("查询服务对象列表")
    @GetMapping("/list")
    public Result<List<ServiceSubject>> getSubjectList() {
        Long userId = UserContextHolder.getUserId();
        List<ServiceSubject> list = serviceSubjectService.getSubjectList(userId);
        return Result.success(list);
    }

    @ApiOperation("查询服务对象详情")
    @GetMapping("/{subjectId}")
    public Result<ServiceSubject> getSubjectDetail(
            @ApiParam(value = "服务对象ID", required = true) @PathVariable Long subjectId) {
        Long userId = UserContextHolder.getUserId();
        ServiceSubject subject = serviceSubjectService.getSubjectDetail(userId, subjectId);
        return Result.success(subject);
    }
}
