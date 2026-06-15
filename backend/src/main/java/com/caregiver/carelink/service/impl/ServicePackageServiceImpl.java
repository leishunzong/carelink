package com.caregiver.carelink.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.caregiver.carelink.common.exception.BusinessException;
import com.caregiver.carelink.common.result.ResultCode;
import com.caregiver.carelink.dto.ServicePackageDTO;
import com.caregiver.carelink.entity.CaregiverServiceConfig;
import com.caregiver.carelink.entity.ServicePackage;
import com.caregiver.carelink.mapper.CaregiverServiceConfigMapper;
import com.caregiver.carelink.mapper.ServicePackageMapper;
import com.caregiver.carelink.service.ServicePackageService;
import com.caregiver.carelink.vo.CaregiverMyPackageVO;
import com.caregiver.carelink.vo.ServicePackageVO;
import com.caregiver.carelink.vo.ServicePackageWithStatusVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 服务包服务实现类
 *
 * @author CareLink
 * @since 2026-02-11
 */
@Service
public class ServicePackageServiceImpl extends ServiceImpl<ServicePackageMapper, ServicePackage> implements ServicePackageService {

    private static final Logger log = LoggerFactory.getLogger(ServicePackageServiceImpl.class);

    @Resource
    private CaregiverServiceConfigMapper caregiverServiceConfigMapper;

    @Override
    public IPage<ServicePackageVO> pageList(Integer category, Integer status, long current, long size) {
        LambdaQueryWrapper<ServicePackage> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(category != null, ServicePackage::getCategory, category)
                .eq(status != null, ServicePackage::getStatus, status)
                .orderByDesc(ServicePackage::getSales);
        Page<ServicePackage> page = page(new Page<>(current, size), wrapper);
        List<ServicePackageVO> voList = page.getRecords().stream().map(this::entityToVO).collect(Collectors.toList());
        Page<ServicePackageVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        voPage.setRecords(voList);
        return voPage;
    }

    @Override
    public IPage<ServicePackageVO> search(String keyword, Integer category, long current, long size) {
        if (!StringUtils.hasText(keyword)) {
            return pageList(category, 1, current, size);
        }
        Page<ServicePackage> page = new Page<>(current, size);
        IPage<ServicePackage> result = baseMapper.searchByKeyword(page, keyword.trim(), category);
        List<ServicePackageVO> voList = result.getRecords().stream().map(this::entityToVO).collect(Collectors.toList());
        Page<ServicePackageVO> voPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        voPage.setRecords(voList);
        return voPage;
    }

    @Override
    public List<String> getHotKeywords(int limit) {
        int size = Math.min(Math.max(limit, 1), 20);
        LambdaQueryWrapper<ServicePackage> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ServicePackage::getStatus, 1)
                .select(ServicePackage::getName)
                .orderByDesc(ServicePackage::getSales)
                .last("LIMIT " + size);
        List<ServicePackage> list = list(wrapper);
        return list.stream()
                .map(ServicePackage::getName)
                .filter(StringUtils::hasText)
                .collect(Collectors.toList());
    }

    @Override
    public List<ServicePackageVO> getCaregiverPackages(Long caregiverId) {
        LambdaQueryWrapper<CaregiverServiceConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CaregiverServiceConfig::getCaregiverId, caregiverId);
        List<CaregiverServiceConfig> configs = caregiverServiceConfigMapper.selectList(wrapper);
        if (configs == null || configs.isEmpty()) {
            return Collections.emptyList();
        }
        List<ServicePackageVO> voList = new ArrayList<>();
        for (CaregiverServiceConfig config : configs) {
            ServicePackage entity = getById(config.getPackageId());
            if (entity == null) {
                continue;
            }
            // 仅返回上架服务包
            if (entity.getStatus() != null && entity.getStatus() == 1) {
                voList.add(entityToVO(entity));
            }
        }
        return voList;
    }

    @Override
    public List<CaregiverMyPackageVO> getCaregiverMyPackages(Long caregiverId) {
        LambdaQueryWrapper<CaregiverServiceConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CaregiverServiceConfig::getCaregiverId, caregiverId)
                .orderByDesc(CaregiverServiceConfig::getCreateTime);
        List<CaregiverServiceConfig> configs = caregiverServiceConfigMapper.selectList(wrapper);
        if (configs == null || configs.isEmpty()) {
            return Collections.emptyList();
        }
        List<CaregiverMyPackageVO> voList = new ArrayList<>();
        for (CaregiverServiceConfig config : configs) {
            ServicePackage entity = getById(config.getPackageId());
            if (entity == null) {
                continue;
            }
            if (entity.getStatus() != null && entity.getStatus() == 1) {
                CaregiverMyPackageVO vo = new CaregiverMyPackageVO();
                vo.setId(entity.getId());
                vo.setName(entity.getName());
                vo.setCategory(entity.getCategory());
                vo.setCoverImage(entity.getCoverImage());
                vo.setDescription(entity.getDescription());
                vo.setBindTime(config.getCreateTime());
                voList.add(vo);
            }
        }
        return voList;
    }

    @Override
    public ServicePackageVO getDetailById(Long id) {
        ServicePackage entity = getById(id);
        if (entity == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "服务包不存在");
        }
        return entityToVO(entity);
    }

    @Override
    public void addPackage(ServicePackageDTO dto) {
        ServicePackage entity = dtoToEntity(dto);
        entity.setId(null);
        entity.setSales(dto.getSales() != null ? dto.getSales() : 0);
        entity.setAllowMonth(dto.getAllowMonth() != null ? dto.getAllowMonth() : 0);
        entity.setAllowDay(dto.getAllowDay() != null ? dto.getAllowDay() : 0);
        entity.setAllowHour(dto.getAllowHour() != null ? dto.getAllowHour() : 0);
        entity.setAllowTimes(dto.getAllowTimes() != null ? dto.getAllowTimes() : 0);
        entity.setStatus(dto.getStatus() != null ? dto.getStatus() : 1);
        save(entity);
        log.info("新增服务包 id={}, name={}, category={}", entity.getId(), entity.getName(), entity.getCategory());
    }

    @Override
    public void updatePackageById(Long id, ServicePackageDTO dto) {
        ServicePackage entity = getById(id);
        if (entity == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "服务包不存在");
        }
        ServicePackage update = dtoToEntity(dto);
        update.setId(id);
        update.setCreateTime(entity.getCreateTime());
        if (dto.getSales() != null) {
            update.setSales(dto.getSales());
        } else {
            update.setSales(entity.getSales());
        }
        update.setAllowMonth(dto.getAllowMonth() != null ? dto.getAllowMonth() : entity.getAllowMonth());
        update.setAllowDay(dto.getAllowDay() != null ? dto.getAllowDay() : entity.getAllowDay());
        update.setAllowHour(dto.getAllowHour() != null ? dto.getAllowHour() : entity.getAllowHour());
        update.setAllowTimes(dto.getAllowTimes() != null ? dto.getAllowTimes() : entity.getAllowTimes());
        update.setStatus(dto.getStatus() != null ? dto.getStatus() : entity.getStatus());
        updateById(update);
        log.info("更新服务包 id={}, name={}", id, update.getName());
    }

    @Override
    public boolean removePackageById(Long id) {
        log.info("删除服务包 id={}", id);
        if (!removeById(id)) {
            throw new BusinessException(ResultCode.NOT_FOUND, "服务包不存在");
        }
        return false;
    }

    @Override
    public void onShelf(Long id) {
        ServicePackage entity = getById(id);
        if (entity == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "服务包不存在");
        }
        entity.setStatus(1);
        updateById(entity);
        log.info("服务包上架 id={}, name={}", id, entity.getName());
    }

    @Override
    public void offShelf(Long id) {
        ServicePackage entity = getById(id);
        if (entity == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "服务包不存在");
        }
        entity.setStatus(0);
        updateById(entity);
        log.info("服务包下架 id={}, name={}", id, entity.getName());
    }

    @Override
    public IPage<ServicePackageWithStatusVO> pageListWithStatus(Long caregiverId, Integer category, long current, long size) {
        // 查询上架服务包分页
        LambdaQueryWrapper<ServicePackage> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(category != null, ServicePackage::getCategory, category)
                .eq(ServicePackage::getStatus, 1)
                .orderByDesc(ServicePackage::getSales);
        Page<ServicePackage> page = page(new Page<>(current, size), wrapper);

        // 查询护工已开通的服务包，建立 packageId -> createTime 映射
        LambdaQueryWrapper<CaregiverServiceConfig> configWrapper = new LambdaQueryWrapper<>();
        configWrapper.eq(CaregiverServiceConfig::getCaregiverId, caregiverId);
        List<CaregiverServiceConfig> configs = caregiverServiceConfigMapper.selectList(configWrapper);
        Map<Long, CaregiverServiceConfig> configMap = new HashMap<>();
        for (CaregiverServiceConfig config : configs) {
            configMap.put(config.getPackageId(), config);
        }

        // 组装结果
        List<ServicePackageWithStatusVO> voList = new ArrayList<>();
        for (ServicePackage entity : page.getRecords()) {
            ServicePackageWithStatusVO vo = entityToWithStatusVO(entity);
            CaregiverServiceConfig config = configMap.get(entity.getId());
            if (config != null) {
                vo.setOpened(true);
                vo.setBindTime(config.getCreateTime());
            } else {
                vo.setOpened(false);
            }
            voList.add(vo);
        }

        Page<ServicePackageWithStatusVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        voPage.setRecords(voList);
        return voPage;
    }

    private ServicePackageWithStatusVO entityToWithStatusVO(ServicePackage entity) {
        ServicePackageWithStatusVO vo = new ServicePackageWithStatusVO();
        BeanUtil.copyProperties(entity, vo, "mandatorySkills");
        if (StringUtils.hasText(entity.getMandatorySkills())) {
            List<Long> ids = Stream.of(entity.getMandatorySkills().split(","))
                    .filter(s -> !s.trim().isEmpty())
                    .map(s -> Long.parseLong(s.trim()))
                    .collect(Collectors.toList());
            vo.setMandatorySkillIds(ids);
        } else {
            vo.setMandatorySkillIds(Collections.emptyList());
        }
        return vo;
    }

    private ServicePackage dtoToEntity(ServicePackageDTO dto) {
        ServicePackage entity = new ServicePackage();
        BeanUtil.copyProperties(dto, entity, "mandatorySkillIds", "sales", "allowMonth", "allowDay", "allowHour", "allowTimes", "status");
        if (!CollectionUtils.isEmpty(dto.getMandatorySkillIds())) {
            entity.setMandatorySkills(dto.getMandatorySkillIds().stream()
                    .map(String::valueOf)
                    .collect(Collectors.joining(",")));
        } else {
            entity.setMandatorySkills(null);
        }
        return entity;
    }

    private ServicePackageVO entityToVO(ServicePackage entity) {
        ServicePackageVO vo = new ServicePackageVO();
        BeanUtil.copyProperties(entity, vo, "mandatorySkills");
        if (StringUtils.hasText(entity.getMandatorySkills())) {
            List<Long> ids = Stream.of(entity.getMandatorySkills().split(","))
                    .filter(s -> !s.trim().isEmpty())
                    .map(s -> Long.parseLong(s.trim()))
                    .collect(Collectors.toList());
            vo.setMandatorySkillIds(ids);
        } else {
            vo.setMandatorySkillIds(Collections.emptyList());
        }
        return vo;
    }
}
