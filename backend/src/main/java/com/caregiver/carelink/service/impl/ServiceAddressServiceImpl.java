package com.caregiver.carelink.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.caregiver.carelink.common.exception.BusinessException;
import com.caregiver.carelink.common.result.ResultCode;
import com.caregiver.carelink.dto.ServiceAddressDTO;
import com.caregiver.carelink.entity.ServiceAddress;
import com.caregiver.carelink.mapper.ServiceAddressMapper;
import com.caregiver.carelink.service.ServiceAddressService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 服务地址服务实现类
 *
 * @author CareLink
 * @since 2026-01-29
 */
@Service
public class ServiceAddressServiceImpl extends ServiceImpl<ServiceAddressMapper, ServiceAddress> implements ServiceAddressService {

    private static final Logger log = LoggerFactory.getLogger(ServiceAddressServiceImpl.class);

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addAddress(Long userId, ServiceAddressDTO dto) {
        // 如果设置为默认，先取消其他默认
        if (dto.getIsDefault() != null && dto.getIsDefault() == 1) {
            LambdaUpdateWrapper<ServiceAddress> updateWrapper = new LambdaUpdateWrapper<>();
            updateWrapper.eq(ServiceAddress::getUserId, userId)
                    .set(ServiceAddress::getIsDefault, 0);
            update(updateWrapper);
        }

        ServiceAddress address = new ServiceAddress();
        BeanUtil.copyProperties(dto, address);
        address.setUserId(userId);
        save(address);
        log.info("新增服务地址 userId={}, addressId={}", userId, address.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateAddress(Long userId, Long addressId, ServiceAddressDTO dto) {
        // 检查地址是否存在且属于当前用户
        ServiceAddress address = getById(addressId);
        if (address == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "服务地址不存在");
        }
        if (!address.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "无权操作该服务地址");
        }

        // 如果设置为默认，先取消其他默认
        if (dto.getIsDefault() != null && dto.getIsDefault() == 1) {
            LambdaUpdateWrapper<ServiceAddress> updateWrapper = new LambdaUpdateWrapper<>();
            updateWrapper.eq(ServiceAddress::getUserId, userId)
                    .ne(ServiceAddress::getId, addressId)
                    .set(ServiceAddress::getIsDefault, 0);
            update(updateWrapper);
        }

        BeanUtil.copyProperties(dto, address, "id", "userId");
        updateById(address);
    }

    @Override
    public void deleteAddress(Long userId, Long addressId) {
        // 检查地址是否存在且属于当前用户
        ServiceAddress address = getById(addressId);
        if (address == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "服务地址不存在");
        }
        if (!address.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "无权删除该服务地址");
        }

        removeById(addressId);
        log.info("删除服务地址 userId={}, addressId={}", userId, addressId);
    }

    @Override
    public List<ServiceAddress> getAddressList(Long userId) {
        LambdaQueryWrapper<ServiceAddress> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ServiceAddress::getUserId, userId)
                .orderByDesc(ServiceAddress::getIsDefault)
                .orderByDesc(ServiceAddress::getCreateTime);
        return list(wrapper);
    }

    @Override
    public ServiceAddress getAddressDetail(Long userId, Long addressId) {
        ServiceAddress address = getById(addressId);
        if (address == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "服务地址不存在");
        }
        if (!address.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "无权查看该服务地址");
        }
        return address;
    }
}
