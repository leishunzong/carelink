package com.caregiver.carelink.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * 技能字典DTO
 *
 * @author CareLink
 * @since 2026-01-29
 */
@Data
@ApiModel(description = "技能字典信息")
public class SkillDictDTO {

    @ApiModelProperty(value = "技能名称", required = true)
    @NotBlank(message = "技能名称不能为空")
    private String skillName;

    @ApiModelProperty("技能分类：1-临床医疗护理 2-基础生活照料 3-康复训练与介护 4-失智专项护理 5-居家安全与应急 6-精神慰藉与社交")
    private Integer skillType;

    @ApiModelProperty("技能详细描述")
    private String description;

    @ApiModelProperty("护工添加该技能时是否需审核：0-否(默认通过) 1-是")
    private Integer needAudit;
}
