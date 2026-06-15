package com.caregiver.carelink.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * 护工上门打卡请求（需上传当前定位，用于校验与服务地址距离）
 *
 * @author CareLink
 * @since 2026-02-11
 */
@Data
@ApiModel(description = "上门打卡请求")
public class StartServiceDTO {

    @NotNull(message = "当前经度不能为空")
    @ApiModelProperty(value = "护工当前经度", required = true, example = "116.397128")
    private BigDecimal longitude;

    @NotNull(message = "当前纬度不能为空")
    @ApiModelProperty(value = "护工当前纬度", required = true, example = "39.916527")
    private BigDecimal latitude;
}
