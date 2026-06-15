package com.caregiver.carelink.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * 评价标签DTO
 *
 * @author CareLink
 * @since 2026-01-29
 */
@Data
@ApiModel(description = "评价标签信息")
public class ReviewTagDTO {

    @ApiModelProperty(value = "标签名称", required = true)
    @NotBlank(message = "标签名称不能为空")
    private String name;

    @ApiModelProperty(value = "标签类型：1-好评标签, 2-差评标签", required = true)
    @NotNull(message = "标签类型不能为空")
    private Integer type;

    @ApiModelProperty("排序（数字越小越靠前）")
    private Integer sort;
}
