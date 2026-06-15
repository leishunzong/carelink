package com.caregiver.carelink.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 护工技能实体类
 *
 * @author CareLink
 * @since 2026-01-29
 */
@Data
@TableName("caregiver_skill")
@ApiModel(description = "护工技能实体")
public class CaregiverSkill implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty("主键ID")
    @TableId(type = IdType.AUTO)
    private Long id;

    @ApiModelProperty("护工ID")
    private Long caregiverId;

    @ApiModelProperty("技能字典ID")
    private Long skillId;

    @ApiModelProperty("技能证书照片")
    private String certImage;

    @ApiModelProperty("审核状态：0-待审核 1-通过 2-拒绝")
    private Integer auditStatus;

    @ApiModelProperty("创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @ApiModelProperty("更新时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}

