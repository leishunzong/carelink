package com.caregiver.carelink.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * 创建评价DTO
 *
 * @author CareLink
 * @since 2026-01-29
 */
@Data
@ApiModel(description = "创建评价请求")
public class ReviewCreateDTO {

    @ApiModelProperty(value = "订单ID", required = true)
    @NotNull(message = "订单ID不能为空")
    private Long orderId;

    @ApiModelProperty(value = "被评价护工ID", required = true)
    @NotNull(message = "护工ID不能为空")
    private Long caregiverId;

    @ApiModelProperty("评价内容")
    private String content;

    @ApiModelProperty(value = "评价类型：1-好评, 2-差评", required = true)
    @NotNull(message = "评价类型不能为空")
    private Integer type;

    @ApiModelProperty(value = "星级评分：1-5星", required = true)
    @NotNull(message = "请选择星级")
    @Min(value = 1, message = "星级为1-5星")
    @Max(value = 5, message = "星级为1-5星")
    private Integer stars;

    @ApiModelProperty("是否匿名：0-否, 1-是")
    private Integer isAnonymous;

    @ApiModelProperty("评价标签ID列表")
    private List<Long> tagIds;
}
