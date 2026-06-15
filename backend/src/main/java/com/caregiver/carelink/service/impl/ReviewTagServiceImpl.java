package com.caregiver.carelink.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.caregiver.carelink.common.exception.BusinessException;
import com.caregiver.carelink.common.result.ResultCode;
import com.caregiver.carelink.dto.ReviewTagDTO;
import com.caregiver.carelink.entity.ReviewTag;
import com.caregiver.carelink.mapper.ReviewTagMapper;
import com.caregiver.carelink.service.ReviewTagService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 评价标签服务实现类
 *
 * @author CareLink
 * @since 2026-01-29
 */
@Service
public class ReviewTagServiceImpl extends ServiceImpl<ReviewTagMapper, ReviewTag> implements ReviewTagService {

    private static final Logger log = LoggerFactory.getLogger(ReviewTagServiceImpl.class);

    @Override
    public void addTag(ReviewTagDTO dto) {
        // 检查标签名称是否已存在
        LambdaQueryWrapper<ReviewTag> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ReviewTag::getName, dto.getName());
        if (count(wrapper) > 0) {
            throw new BusinessException("标签名称已存在");
        }

        ReviewTag tag = new ReviewTag();
        BeanUtil.copyProperties(dto, tag);
        if (tag.getSort() == null) {
            tag.setSort(0);
        }
        save(tag);
        log.info("新增评价标签 id={}, name={}, type={}", tag.getId(), dto.getName(), dto.getType());
    }

    @Override
    public void updateTag(Long tagId, ReviewTagDTO dto) {
        ReviewTag tag = getById(tagId);
        if (tag == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "标签不存在");
        }

        // 检查名称是否与其他标签重复
        LambdaQueryWrapper<ReviewTag> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ReviewTag::getName, dto.getName())
                .ne(ReviewTag::getId, tagId);
        if (count(wrapper) > 0) {
            throw new BusinessException("标签名称已存在");
        }

        BeanUtil.copyProperties(dto, tag, "id");
        updateById(tag);
        log.info("更新评价标签 id={}, name={}", tagId, dto.getName());
    }

    @Override
    public void deleteTag(Long tagId) {
        ReviewTag tag = getById(tagId);
        if (tag == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "标签不存在");
        }
        removeById(tagId);
        log.info("删除评价标签 id={}, name={}", tagId, tag.getName());
    }

    @Override
    public List<ReviewTag> getAllTags() {
        LambdaQueryWrapper<ReviewTag> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByAsc(ReviewTag::getType)
                .orderByAsc(ReviewTag::getSort);
        return list(wrapper);
    }

    @Override
    public List<ReviewTag> getTagsByType(Integer type) {
        LambdaQueryWrapper<ReviewTag> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ReviewTag::getType, type)
                .orderByAsc(ReviewTag::getSort);
        return list(wrapper);
    }
}
