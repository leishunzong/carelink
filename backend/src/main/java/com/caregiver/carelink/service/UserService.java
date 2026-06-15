package com.caregiver.carelink.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.caregiver.carelink.dto.LoginDTO;
import com.caregiver.carelink.dto.UserRegisterDTO;
import com.caregiver.carelink.dto.UserUpdateDTO;
import com.caregiver.carelink.entity.User;
import com.caregiver.carelink.vo.LoginVO;
import com.caregiver.carelink.vo.UserInfoVO;

/**
 * 用户服务接口
 *
 * @author CareLink
 * @since 2026-01-29
 */
public interface UserService extends IService<User> {

    /**
     * 用户注册
     */
    void register(UserRegisterDTO dto);

    /**
     * 用户登录
     */
    LoginVO login(LoginDTO dto);

    /**
     * 获取用户信息
     */
    UserInfoVO getUserInfo(Long userId);

    /**
     * 更新用户信息（包括头像）
     */
    void updateUserInfo(Long userId, UserUpdateDTO dto);

    /**
     * 设置用户城市
     */
    void setUserCity(Long userId, String cityCode, String cityName);

    /**
     * 修改密码
     */
    void updatePassword(Long userId, String oldPassword, String newPassword);
}
