package com.caregiver.carelink.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.caregiver.carelink.dto.ReviewTagDTO;
import com.caregiver.carelink.entity.ReviewTag;

import java.util.List;

/**
 * 评价标签服务接口
 *
 * @author CareLink
 * @since 2026-01-29
 */
public interface ReviewTagService extends IService<ReviewTag> {

    /**
     * 新增标签
     */
    void addTag(ReviewTagDTO dto);

    /**
     * 修改标签
     */
    void updateTag(Long tagId, ReviewTagDTO dto);

    /**
     * 删除标签
     */
    void deleteTag(Long tagId);

    /**
     * 查询所有标签
     */
    List<ReviewTag> getAllTags();

    /**
     * 按类型查询标签
     */
    List<ReviewTag> getTagsByType(Integer type);
}
