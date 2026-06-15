package com.caregiver.carelink.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.caregiver.carelink.entity.ServicePackage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 服务包Mapper接口
 *
 * @author CareLink
 * @since 2026-02-11
 */
@Mapper
public interface ServicePackageMapper extends BaseMapper<ServicePackage> {

    /**
     * 按关键词全文检索服务包（仅上架），按相关度、销量排序
     */
    IPage<ServicePackage> searchByKeyword(Page<ServicePackage> page, @Param("keyword") String keyword, @Param("category") Integer category);
}
