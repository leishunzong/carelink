package com.caregiver.carelink.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.caregiver.carelink.common.exception.BusinessException;
import com.caregiver.carelink.common.result.ResultCode;
import com.caregiver.carelink.dto.LoginDTO;
import com.caregiver.carelink.dto.UserRegisterDTO;
import com.caregiver.carelink.dto.UserUpdateDTO;
import com.caregiver.carelink.entity.User;
import com.caregiver.carelink.mapper.UserMapper;
import com.caregiver.carelink.service.UserService;
import com.caregiver.carelink.utils.JwtUtils;
import com.caregiver.carelink.utils.PasswordUtils;
import com.caregiver.carelink.vo.LoginVO;
import com.caregiver.carelink.vo.UserInfoVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;

/**
 * 用户服务实现类
 *
 * @author CareLink
 * @since 2026-01-29
 */
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    private static final Logger log = LoggerFactory.getLogger(UserServiceImpl.class);

    @Resource
    private JwtUtils jwtUtils;

    @Override
    public void register(UserRegisterDTO dto) {
        log.info("用户注册 username={}, phone={}", dto.getUsername(), dto.getPhone());
        // 检查用户名是否已存在
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, dto.getUsername());
        if (count(wrapper) > 0) {
            throw new BusinessException("用户名已存在");
        }

        // 检查手机号是否已被注册
        wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getPhone, dto.getPhone());
        if (count(wrapper) > 0) {
            throw new BusinessException("手机号已被注册");
        }

        // 创建用户
        User user = new User();
        user.setUsername(dto.getUsername());
        user.setPassword(PasswordUtils.encode(dto.getPassword()));
        user.setNickname(dto.getNickname() != null ? dto.getNickname() : dto.getUsername());
        user.setPhone(dto.getPhone());
        user.setStatus(1); // 默认启用

        save(user);
        log.info("用户注册成功 userId={}, username={}", user.getId(), dto.getUsername());
    }

    @Override
    public LoginVO login(LoginDTO dto) {
        log.info("用户登录 username={}", dto.getUsername());
        // 查询用户
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, dto.getUsername())
                .or()
                .eq(User::getPhone, dto.getUsername());
        User user = getOne(wrapper);

        if (user == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "用户名或密码错误");
        }

        // 验证密码
        if (!PasswordUtils.matches(dto.getPassword(), user.getPassword())) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "用户名或密码错误");
        }

        // 检查账号状态
        if (user.getStatus() == 0) {
            throw new BusinessException(ResultCode.FORBIDDEN, "账号已被禁用");
        }

        // 生成Token
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getId());
        claims.put("userType", "user");
        String token = jwtUtils.generateToken(claims);

        // 返回登录信息
        LoginVO loginVO = LoginVO.builder()
                .token(token)
                .userType("user")
                .build();
        log.info("用户登录成功 userId={}", user.getId());
        return loginVO;
    }

    @Override
    public UserInfoVO getUserInfo(Long userId) {
        User user = getById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "用户不存在");
        }

        UserInfoVO vo = new UserInfoVO();
        BeanUtil.copyProperties(user, vo);
        return vo;
    }

    @Override
    public void updateUserInfo(Long userId, UserUpdateDTO dto) {
        User user = getById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "用户不存在");
        }

        // 更新昵称
        if (dto.getNickname() != null && !dto.getNickname().trim().isEmpty()) {
            user.setNickname(dto.getNickname());
        }

        // 更新头像
        if (dto.getAvatar() != null && !dto.getAvatar().trim().isEmpty()) {
            user.setAvatar(dto.getAvatar());
        }

        // 更新手机号
        if (dto.getPhone() != null && !dto.getPhone().trim().isEmpty()) {
            // 检查手机号是否被其他用户使用
            LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(User::getPhone, dto.getPhone())
                    .ne(User::getId, userId);
            if (count(wrapper) > 0) {
                throw new BusinessException("手机号已被使用");
            }
            user.setPhone(dto.getPhone());
        }

        updateById(user);
    }

    @Override
    public void setUserCity(Long userId, String cityCode, String cityName) {
        User user = getById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "用户不存在");
        }

        user.setCityCode(cityCode);
        user.setCityName(cityName);
        updateById(user);
    }

    @Override
    public void updatePassword(Long userId, String oldPassword, String newPassword) {
        log.info("用户修改密码 userId={}", userId);
        User user = getById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "用户不存在");
        }

        // 验证旧密码
        if (!PasswordUtils.matches(oldPassword, user.getPassword())) {
            throw new BusinessException("旧密码错误");
        }

        // 更新为新密码
        user.setPassword(PasswordUtils.encode(newPassword));
        updateById(user);
        log.info("用户修改密码成功 userId={}", userId);
    }
}
