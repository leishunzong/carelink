package com.caregiver.carelink.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotNull;

/**
 * 护工绑定服务包请求 DTO
 *
 * @author CareLink
 * @since 2026-02-11
 */
@Data
@ApiModel(description = "护工绑定服务包")
public class CaregiverPackageBindDTO {

    @ApiModelProperty(value = "服务包ID", required = true)
    @NotNull(message = "服务包ID不能为空")
    private Long packageId;
}
