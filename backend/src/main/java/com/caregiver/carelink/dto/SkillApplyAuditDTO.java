package com.caregiver.carelink.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotNull;

/**
 * 护工技能申请审核请求
 *
 * @author CareLink
 * @since 2026-02-24
 */
@Data
@ApiModel(description = "护工技能申请审核请求")
public class SkillApplyAuditDTO {

    @ApiModelProperty(value = "护工技能记录ID", required = true)
    @NotNull(message = "技能申请记录ID不能为空")
    private Long caregiverSkillId;

    @ApiModelProperty(value = "是否通过", required = true)
    @NotNull(message = "请选择通过或拒绝")
    private Boolean passed;

    @ApiModelProperty("拒绝原因（可选）")
    private String rejectReason;
}
