package com.caregiver.carelink.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotNull;

/**
 * 护工技能DTO
 *
 * @author CareLink
 * @since 2026-01-29
 */
@Data
@ApiModel(description = "护工技能信息")
public class CaregiverSkillDTO {

    @ApiModelProperty(value = "技能字典ID", required = true)
    @NotNull(message = "技能ID不能为空")
    private Long skillId;

    @ApiModelProperty("技能证书照片URL")
    private String certImage;
}

