package com.caregiver.carelink.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.caregiver.carelink.dto.ServiceSubjectDTO;
import com.caregiver.carelink.entity.ServiceSubject;

import java.util.List;

/**
 * 服务对象服务接口
 *
 * @author CareLink
 * @since 2026-01-29
 */
public interface ServiceSubjectService extends IService<ServiceSubject> {

    /**
     * 新增服务对象
     */
    void addSubject(Long userId, ServiceSubjectDTO dto);

    /**
     * 修改服务对象
     */
    void updateSubject(Long userId, Long subjectId, ServiceSubjectDTO dto);

    /**
     * 删除服务对象
     */
    void deleteSubject(Long userId, Long subjectId);

    /**
     * 查询服务对象列表
     */
    List<ServiceSubject> getSubjectList(Long userId);

    /**
     * 查询服务对象详情
     */
    ServiceSubject getSubjectDetail(Long userId, Long subjectId);
}
