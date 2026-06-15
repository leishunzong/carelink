package com.caregiver.carelink.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 护工技能VO（关联查询技能字典）
 *
 * @author CareLink
 * @since 2026-01-29
 */
@Data
@ApiModel(description = "护工技能信息")
public class CaregiverSkillVO {

    @ApiModelProperty("技能ID")
    private Long id;

    @ApiModelProperty("护工ID")
    private Long caregiverId;

    @ApiModelProperty("技能字典ID")
    private Long skillId;

    @ApiModelProperty("技能名称")
    private String skillName;

    @ApiModelProperty("技能分类：1-临床医疗护理 2-基础生活照料 3-康复训练与介护 4-失智专项护理 5-居家安全与应急 6-精神慰藉与社交")
    private Integer skillType;

    @ApiModelProperty("技能描述")
    private String description;

    @ApiModelProperty("技能证书照片")
    private String certImage;

    @ApiModelProperty("审核状态：0-待审核 1-通过 2-拒绝")
    private Integer auditStatus;

    @ApiModelProperty("创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
