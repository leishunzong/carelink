package com.caregiver.carelink.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 评价与标签关联实体类
 *
 * @author CareLink
 * @since 2026-01-29
 */
@Data
@TableName("review_tag_relation")
@ApiModel(description = "评价标签关联实体")
public class ReviewTagRelation implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty("关联ID")
    @TableId(type = IdType.AUTO)
    private Long id;

    @ApiModelProperty("评价ID")
    private Long reviewId;

    @ApiModelProperty("标签ID")
    private Long tagId;

    @ApiModelProperty("创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
