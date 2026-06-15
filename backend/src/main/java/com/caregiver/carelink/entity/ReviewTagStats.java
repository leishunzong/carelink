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
 * 护工评价标签统计实体类
 *
 * @author CareLink
 * @since 2026-01-29
 */
@Data
@TableName("review_tag_stats")
@ApiModel(description = "评价标签统计实体")
public class ReviewTagStats implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty("统计ID")
    @TableId(type = IdType.AUTO)
    private Long id;

    @ApiModelProperty("护工ID")
    private Long caregiverId;

    @ApiModelProperty("标签ID")
    private Long tagId;

    @ApiModelProperty("标签名称（冗余）")
    private String tagName;

    @ApiModelProperty("标签类型：1-好评, 2-差评（冗余）")
    private Integer tagType;

    @ApiModelProperty("被评价次数")
    private Integer count;

    @ApiModelProperty("创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @ApiModelProperty("更新时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
