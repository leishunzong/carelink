package com.caregiver.carelink.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotNull;

/**
 * 护工入驻申请审核请求
 *
 * @author CareLink
 * @since 2026-02-24
 */
@Data
@ApiModel(description = "护工入驻审核请求")
public class SettleAuditDTO {

    @ApiModelProperty(value = "护工ID", required = true)
    @NotNull(message = "护工ID不能为空")
    private Long caregiverId;

    @ApiModelProperty(value = "是否通过", required = true)
    @NotNull(message = "请选择通过或拒绝")
    private Boolean passed;

    @ApiModelProperty("拒绝原因（拒绝时建议填写）")
    private String rejectReason;
}
