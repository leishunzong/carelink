package com.caregiver.carelink.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * 城市信息DTO
 *
 * @author CareLink
 * @since 2026-01-29
 */
@Data
@ApiModel(description = "城市信息")
public class CityDTO {

    @ApiModelProperty(value = "城市编码", required = true)
    @NotBlank(message = "城市编码不能为空")
    private String cityCode;

    @ApiModelProperty(value = "城市名称", required = true)
    @NotBlank(message = "城市名称不能为空")
    private String cityName;
}
