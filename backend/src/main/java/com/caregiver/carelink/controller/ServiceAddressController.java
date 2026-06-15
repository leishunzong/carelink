package com.caregiver.carelink.controller;

import com.caregiver.carelink.common.context.UserContextHolder;
import com.caregiver.carelink.common.result.Result;
import com.caregiver.carelink.dto.ServiceAddressDTO;
import com.caregiver.carelink.entity.ServiceAddress;
import com.caregiver.carelink.service.ServiceAddressService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

/**
 * 服务地址控制器
 *
 * @author CareLink
 * @since 2026-01-29
 */
@Api(tags = "服务地址管理")
@RestController
@RequestMapping("/user/address")
public class ServiceAddressController {

    @Resource
    private ServiceAddressService serviceAddressService;

    @ApiOperation("新增服务地址")
    @PostMapping
    public Result<Void> addAddress(@Validated @RequestBody ServiceAddressDTO dto) {
        Long userId = UserContextHolder.getUserId();
        serviceAddressService.addAddress(userId, dto);
        return Result.success("添加成功");
    }

    @ApiOperation("修改服务地址")
    @PutMapping("/{addressId}")
    public Result<Void> updateAddress(
            @ApiParam(value = "地址ID", required = true) @PathVariable Long addressId,
            @Validated @RequestBody ServiceAddressDTO dto) {
        Long userId = UserContextHolder.getUserId();
        serviceAddressService.updateAddress(userId, addressId, dto);
        return Result.success("修改成功");
    }

    @ApiOperation("删除服务地址")
    @DeleteMapping("/{addressId}")
    public Result<Void> deleteAddress(
            @ApiParam(value = "地址ID", required = true) @PathVariable Long addressId) {
        Long userId = UserContextHolder.getUserId();
        serviceAddressService.deleteAddress(userId, addressId);
        return Result.success("删除成功");
    }

    @ApiOperation("查询服务地址列表")
    @GetMapping("/list")
    public Result<List<ServiceAddress>> getAddressList() {
        Long userId = UserContextHolder.getUserId();
        List<ServiceAddress> list = serviceAddressService.getAddressList(userId);
        return Result.success(list);
    }

    @ApiOperation("查询服务地址详情")
    @GetMapping("/{addressId}")
    public Result<ServiceAddress> getAddressDetail(
            @ApiParam(value = "地址ID", required = true) @PathVariable Long addressId) {
        Long userId = UserContextHolder.getUserId();
        ServiceAddress address = serviceAddressService.getAddressDetail(userId, addressId);
        return Result.success(address);
    }
}
