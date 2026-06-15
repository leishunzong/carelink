package com.caregiver.carelink.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.caregiver.carelink.dto.ServiceAddressDTO;
import com.caregiver.carelink.entity.ServiceAddress;

import java.util.List;

/**
 * 服务地址服务接口
 *
 * @author CareLink
 * @since 2026-01-29
 */
public interface ServiceAddressService extends IService<ServiceAddress> {

    /**
     * 新增服务地址
     */
    void addAddress(Long userId, ServiceAddressDTO dto);

    /**
     * 修改服务地址
     */
    void updateAddress(Long userId, Long addressId, ServiceAddressDTO dto);

    /**
     * 删除服务地址
     */
    void deleteAddress(Long userId, Long addressId);

    /**
     * 查询服务地址列表
     */
    List<ServiceAddress> getAddressList(Long userId);

    /**
     * 查询服务地址详情
     */
    ServiceAddress getAddressDetail(Long userId, Long addressId);
}
