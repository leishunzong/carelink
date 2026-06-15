package com.caregiver.carelink.controller;

import com.caregiver.carelink.common.context.UserContextHolder;
import com.caregiver.carelink.common.result.Result;
import com.caregiver.carelink.dto.*;
import com.caregiver.carelink.service.UserService;
import com.caregiver.carelink.vo.LoginVO;
import com.caregiver.carelink.vo.UserInfoVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

/**
 * 用户控制器
 *
 * @author CareLink
 * @since 2026-01-29
 */
@Api(tags = "用户管理")
@RestController
@RequestMapping("/user")
public class UserController {

    @Resource
    private UserService userService;

    @ApiOperation("用户注册")
    @PostMapping("/register")
    public Result<Void> register(@Validated @RequestBody UserRegisterDTO dto) {
        userService.register(dto);
        return Result.success("注册成功");
    }

    @ApiOperation("用户登录")
    @PostMapping("/login")
    public Result<LoginVO> login(@Validated @RequestBody LoginDTO dto) {
        LoginVO loginVO = userService.login(dto);
        return Result.success(loginVO);
    }

    @ApiOperation("获取我的信息")
    @GetMapping("/info")
    public Result<UserInfoVO> getMyInfo() {
        // 自动获取当前登录用户ID
        Long userId = UserContextHolder.getUserId();
        UserInfoVO userInfo = userService.getUserInfo(userId);
        return Result.success(userInfo);
    }

    @ApiOperation("修改我的信息")
    @PutMapping("/info")
    public Result<Void> updateMyInfo(@Validated @RequestBody UserUpdateDTO dto) {
        // 自动获取当前登录用户ID
        Long userId = UserContextHolder.getUserId();
        userService.updateUserInfo(userId, dto);
        return Result.success("修改成功");
    }

    @ApiOperation("设置我的城市（登录后首次使用）")
    @PostMapping("/city")
    public Result<Void> setMyCity(@Validated @RequestBody CityDTO dto) {
        Long userId = UserContextHolder.getUserId();
        userService.setUserCity(userId, dto.getCityCode(), dto.getCityName());
        return Result.success("城市设置成功");
    }

    @ApiOperation("修改密码")
    @PutMapping("/password")
    public Result<Void> updatePassword(@Validated @RequestBody PasswordUpdateDTO dto) {
        Long userId = UserContextHolder.getUserId();
        userService.updatePassword(userId, dto.getOldPassword(), dto.getNewPassword());
        return Result.success("密码修改成功");
    }
}

