package com.caregiver.carelink.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.caregiver.carelink.entity.CaregiverServiceConfig;

/**
 * 护工服务包准入关联服务
 *
 * @author CareLink
 * @since 2026-02-11
 */
public interface CaregiverServiceConfigService extends IService<CaregiverServiceConfig> {

    /**
     * 为护工添加服务包准入（关联护工与服务包，并维护 Redis 集合）
     */
    void addAssociation(Long caregiverId, Long packageId);

    /**
     * 取消护工对该服务包的准入（删除关联并从 Redis 集合移除）
     */
    void removeAssociation(Long caregiverId, Long packageId);
}
