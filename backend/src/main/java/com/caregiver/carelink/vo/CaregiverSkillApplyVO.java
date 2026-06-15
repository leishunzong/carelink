package com.caregiver.carelink.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 护工技能申请列表项（管理员端）
 *
 * @author CareLink
 * @since 2026-02-24
 */
@Data
@ApiModel(description = "护工技能申请")
public class CaregiverSkillApplyVO {

    @ApiModelProperty("护工技能记录ID")
    private Long id;

    @ApiModelProperty("护工ID")
    private Long caregiverId;

    @ApiModelProperty("护工姓名")
    private String caregiverName;

    @ApiModelProperty("护工手机号")
    private String caregiverPhone;

    @ApiModelProperty("技能字典ID")
    private Long skillId;

    @ApiModelProperty("技能名称")
    private String skillName;

    @ApiModelProperty("技能证书照片URL")
    private String certImage;

    @ApiModelProperty("审核状态：0-待审核 1-通过 2-拒绝")
    private Integer auditStatus;

    @ApiModelProperty("申请时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
