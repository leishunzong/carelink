package com.caregiver.carelink.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;

/**
 * 服务包创建/更新 DTO
 *
 * @author CareLink
 * @since 2026-02-11
 */
@Data
@ApiModel(description = "服务包创建/更新参数")
public class ServicePackageDTO {

    @ApiModelProperty(value = "服务包名称", required = true)
    @NotBlank(message = "服务包名称不能为空")
    private String name;

    @ApiModelProperty(value = "服务类型：1-居家陪护 2-医院陪护 3-周期护理 4-家政服务 5-陪诊服务 6-母婴护理", required = true)
    @NotNull(message = "服务类型不能为空")
    private Integer category;

    @ApiModelProperty("服务包封面图片URL")
    private String coverImage;

    @ApiModelProperty("服务包简介（列表/卡片展示）")
    private String description;

    @ApiModelProperty("服务包详情（具体服务内容、标准、流程等，富文本）")
    private String detail;

    @ApiModelProperty("销量（可选，默认0）")
    private Integer sales;

    @ApiModelProperty("是否支持按月服务：0-否 1-是")
    private Integer allowMonth;

    @ApiModelProperty("是否支持按天服务：0-否 1-是")
    private Integer allowDay;

    @ApiModelProperty("是否支持按小时服务：0-否 1-是")
    private Integer allowHour;

    @ApiModelProperty("是否支持按次数服务：0-否 1-是")
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
}
