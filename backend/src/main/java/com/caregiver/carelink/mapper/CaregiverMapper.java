package com.caregiver.carelink.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.caregiver.carelink.entity.Caregiver;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;

/**
 * 护工Mapper接口
 *
 * @author CareLink
 * @since 2026-01-29
 */
@Mapper
public interface CaregiverMapper extends BaseMapper<Caregiver> {

    /**
     * 分页搜索护工（支持关联统计表排序）
     *
     * @param page 分页对象
     * @param cityCode 城市编码
     * @param gender 性别
     * @param minBirthday 最大生日（对应最小年龄）
     * @param maxBirthday 最小生日（对应最大年龄）
     * @param minWorkYears 最小从业年限
     * @param maxWorkYears 最大从业年限
     * @param education 学历
     * @param sortField 排序字段
     * @param sortOrder 排序方向
     * @return 护工分页结果
     */
    Page<Caregiver> searchCaregivers(Page<Caregiver> page,
                                     @Param("cityCode") String cityCode,
                                     @Param("gender") Integer gender,
                                     @Param("nameKeyword") String nameKeyword,
                                     @Param("minBirthday") LocalDate minBirthday,
                                     @Param("maxBirthday") LocalDate maxBirthday,
                                     @Param("minWorkYears") Integer minWorkYears,
                                     @Param("maxWorkYears") Integer maxWorkYears,
                                     @Param("education") String education,
                                     @Param("packageCategory") Integer packageCategory,
                                     @Param("sortField") String sortField,
                                     @Param("sortOrder") String sortOrder);
}
