package com.caregiver.carelink.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;

/**
 * 护工搜索条件DTO
 *
 * @author CareLink
 * @since 2026-02-11
 */
@Data
@ApiModel(description = "护工搜索条件")
public class CaregiverSearchDTO {

    @ApiModelProperty(value = "城市编码（必传，前端隐藏字段）", required = true)
    @NotBlank(message = "城市编码不能为空")
    private String cityCode;

    // ==================== 筛选条件 ====================

    @ApiModelProperty("性别筛选: 1男 2女")
    private Integer gender;

    @ApiModelProperty("护工姓名关键字（模糊匹配真实姓名）")
    private String nameKeyword;

    @ApiModelProperty("最小年龄")
    @Min(value = 18, message = "最小年龄不能小于18")
    private Integer minAge;

    @ApiModelProperty("最大年龄")
    @Max(value = 80, message = "最大年龄不能大于80")
    private Integer maxAge;

    @ApiModelProperty("最小从业年限")
    @Min(value = 0, message = "最小从业年限不能小于0")
    private Integer minWorkYears;

    @ApiModelProperty("最大从业年限")
    @Max(value = 50, message = "最大从业年限不能大于50")
    private Integer maxWorkYears;

    @ApiModelProperty("学历筛选")
    private String education;

    @ApiModelProperty("服务包类型：1-居家陪护 2-医院陪护 3-周期护理 4-家政服务 5-陪诊服务 6-母婴护理（筛选可提供该类服务的护工）")
    private Integer packageCategory;

    // ==================== 排序条件 ====================

    @ApiModelProperty("排序字段: orderCount-完单量, goodReviewRate-好评率, workYears-从业年限, createTime-注册时间")
    private String sortField;

    @ApiModelProperty("排序方向: ASC-升序, DESC-降序")
    private String sortOrder = "DESC";

    // ==================== 分页条件 ====================

    @ApiModelProperty("页码（从1开始）")
    @Min(value = 1, message = "页码不能小于1")
    private Integer page = 1;

    @ApiModelProperty("每页大小")
    @Min(value = 1, message = "每页大小不能小于1")
    @Max(value = 50, message = "每页大小不能大于50")
    private Integer size = 10;
}