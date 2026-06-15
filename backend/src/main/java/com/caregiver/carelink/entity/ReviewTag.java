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
 * 评价标签实体类
 *
 * @author CareLink
 * @since 2026-01-29
 */
@Data
@TableName("review_tag")
@ApiModel(description = "评价标签实体")
public class ReviewTag implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty("标签ID")
    @TableId(type = IdType.AUTO)
    private Long id;

    @ApiModelProperty("标签名称")
    private String name;

    @ApiModelProperty("标签类型：1-好评标签, 2-差评标签")
    private Integer type;

    @ApiModelProperty("排序")
    private Integer sort;

    @ApiModelProperty("创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @ApiModelProperty("更新时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
