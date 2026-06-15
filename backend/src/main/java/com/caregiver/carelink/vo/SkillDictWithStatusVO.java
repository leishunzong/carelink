package com.caregiver.carelink.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 技能字典（附带当前护工申请状态）VO
 * <p>
 * 用于护工端查看技能列表，标记已申请/未申请的技能
 *
 * @author CareLink
 * @since 2026-03-14
 */
@Data
@ApiModel(description = "技能字典（附带护工申请状态）")
public class SkillDictWithStatusVO {

    @ApiModelProperty("技能字典ID")
    private Long id;

    @ApiModelProperty("技能名称")
    private String skillName;

    @ApiModelProperty("技能分类：1-临床医疗护理 2-基础生活照料 3-康复训练与介护 4-失智专项护理 5-居家安全与应急 6-精神慰藉与社交")
    private Integer skillType;

    @ApiModelProperty("技能描述")
    private String description;

    @ApiModelProperty("护工添加该技能时是否需审核：0-否 1-是")
    private Integer needAudit;

    @ApiModelProperty("当前护工是否已申请该技能")
    private Boolean applied;

    @ApiModelProperty("审核状态：0-待审核 1-通过 2-拒绝（未申请时为null）")
    private Integer auditStatus;

    @ApiModelProperty("技能证书照片（未申请时为null）")
    private String certImage;

    @ApiModelProperty("申请时间（未申请时为null）")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime applyTime;
}
