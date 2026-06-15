package com.caregiver.carelink.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 服务包（附带当前护工开通状态）VO
 * <p>
 * 用于护工端查看服务包列表，标记已开通/未开通的服务包
 *
 * @author CareLink
 * @since 2026-03-14
 */
@Data
@ApiModel(description = "服务包（附带护工开通状态）")
public class ServicePackageWithStatusVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty("服务包ID")
    private Long id;

    @ApiModelProperty("服务包名称")
    private String name;

    @ApiModelProperty("服务类型：1-居家陪护 2-医院陪护 3-周期护理 4-家政服务 5-陪诊服务 6-母婴护理")
    private Integer category;

    @ApiModelProperty("服务包封面图片URL")
    private String coverImage;

    @ApiModelProperty("服务包简介")
    private String description;

    @ApiModelProperty("服务包详情")
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

    @ApiModelProperty("要求的技能ID列表")
    private List<Long> mandatorySkillIds;

    @ApiModelProperty("状态：1-上架 0-下架")
    private Integer status;

    @ApiModelProperty("当前护工是否已开通该服务包")
    private Boolean opened;

    @ApiModelProperty("开通时间（未开通时为null）")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime bindTime;

    @ApiModelProperty("创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @ApiModelProperty("更新时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
