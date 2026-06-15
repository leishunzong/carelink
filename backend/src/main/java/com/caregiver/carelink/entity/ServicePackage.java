package com.caregiver.carelink.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 服务包模板实体类
 *
 * @author CareLink
 * @since 2026-02-11
 */
@Data
@TableName("service_package")
@ApiModel(description = "服务包模板")
public class ServicePackage implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty("主键ID")
    @TableId(type = IdType.AUTO)
    private Long id;

    @ApiModelProperty("服务包名称")
    private String name;

    @ApiModelProperty("服务类型：1-居家陪护 2-医院陪护 3-周期护理 4-家政服务 5-陪诊服务 6-母婴护理")
    private Integer category;

    @ApiModelProperty("服务包封面图片URL")
    private String coverImage;

    @ApiModelProperty("服务包简介（列表/卡片展示）")
    private String description;

    @ApiModelProperty("服务包详情（具体服务内容、标准、流程等）")
    private String detail;

    @ApiModelProperty("销量")
    private Integer sales;

    @ApiModelProperty("是否支持按月服务")
    private Integer allowMonth;

    @ApiModelProperty("是否支持按天服务")
    private Integer allowDay;

    @ApiModelProperty("是否支持按小时服务")
    private Integer allowHour;

    @ApiModelProperty("是否支持按次数服务")
    private Integer allowTimes;

    @ApiModelProperty("按月单价")
    private BigDecimal priceMonth;

    @ApiModelProperty("按天单价")
    private BigDecimal priceDay;

    @ApiModelProperty("按小时单价")
    private BigDecimal priceHour;

    @ApiModelProperty("按次数单价")
    private BigDecimal priceTimes;

    @ApiModelProperty("要求的技能ID列表，逗号分隔")
    private String mandatorySkills;

    @ApiModelProperty("状态：1-上架 0-下架")
    private Integer status;

    @ApiModelProperty("创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @ApiModelProperty("更新时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
