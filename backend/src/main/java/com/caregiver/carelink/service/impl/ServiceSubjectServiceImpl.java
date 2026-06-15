package com.caregiver.carelink.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.caregiver.carelink.common.exception.BusinessException;
import com.caregiver.carelink.common.result.ResultCode;
import com.caregiver.carelink.dto.ServiceSubjectDTO;
import com.caregiver.carelink.entity.ServiceSubject;
import com.caregiver.carelink.mapper.ServiceSubjectMapper;
import com.caregiver.carelink.service.ServiceSubjectService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 服务对象服务实现类
 *
 * @author CareLink
 * @since 2026-01-29
 */
@Service
public class ServiceSubjectServiceImpl extends ServiceImpl<ServiceSubjectMapper, ServiceSubject> implements ServiceSubjectService {

    private static final Logger log = LoggerFactory.getLogger(ServiceSubjectServiceImpl.class);

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addSubject(Long userId, ServiceSubjectDTO dto) {
        // 如果设置为默认，先取消其他默认
        if (dto.getIsDefault() != null && dto.getIsDefault() == 1) {
            LambdaUpdateWrapper<ServiceSubject> updateWrapper = new LambdaUpdateWrapper<>();
            updateWrapper.eq(ServiceSubject::getUserId, userId)
                    .set(ServiceSubject::getIsDefault, 0);
            update(updateWrapper);
        }

        ServiceSubject subject = new ServiceSubject();
        BeanUtil.copyProperties(dto, subject);
        subject.setUserId(userId);
        save(subject);
        log.info("新增服务对象 userId={}, subjectId={}, name={}", userId, subject.getId(), dto.getName());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateSubject(Long userId, Long subjectId, ServiceSubjectDTO dto) {
        // 检查服务对象是否存在且属于当前用户
        ServiceSubject subject = getById(subjectId);
        if (subject == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "服务对象不存在");
        }
        if (!subject.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "无权操作该服务对象");
        }

        // 如果设置为默认，先取消其他默认
        if (dto.getIsDefault() != null && dto.getIsDefault() == 1) {
            LambdaUpdateWrapper<ServiceSubject> updateWrapper = new LambdaUpdateWrapper<>();
            updateWrapper.eq(ServiceSubject::getUserId, userId)
                    .ne(ServiceSubject::getId, subjectId)
                    .set(ServiceSubject::getIsDefault, 0);
            update(updateWrapper);
        }

        BeanUtil.copyProperties(dto, subject, "id", "userId");
        updateById(subject);
    }

    @Override
    public void deleteSubject(Long userId, Long subjectId) {
        // 检查服务对象是否存在且属于当前用户
        ServiceSubject subject = getById(subjectId);
        if (subject == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "服务对象不存在");
        }
        if (!subject.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "无权删除该服务对象");
        }

        removeById(subjectId);
        log.info("删除服务对象 userId={}, subjectId={}", userId, subjectId);
    }

    @Override
    public List<ServiceSubject> getSubjectList(Long userId) {
        LambdaQueryWrapper<ServiceSubject> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ServiceSubject::getUserId, userId)
                .orderByDesc(ServiceSubject::getIsDefault)
                .orderByDesc(ServiceSubject::getCreateTime);
        return list(wrapper);
    }

    @Override
    public ServiceSubject getSubjectDetail(Long userId, Long subjectId) {
        ServiceSubject subject = getById(subjectId);
        if (subject == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "服务对象不存在");
        }
        if (!subject.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "无权查看该服务对象");
        }
        return subject;
    }
}
