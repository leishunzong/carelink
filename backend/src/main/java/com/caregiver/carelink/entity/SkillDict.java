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
 * 技能字典实体类
 *
 * @author CareLink
 * @since 2026-01-29
 */
@Data
@TableName("skill_dict")
@ApiModel(description = "技能字典实体")
public class SkillDict implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty("技能ID")
    @TableId(type = IdType.AUTO)
    private Long id;

    @ApiModelProperty("技能名称")
    private String skillName;

    @ApiModelProperty("技能分类：1-临床医疗护理 2-基础生活照料 3-康复训练与介护 4-失智专项护理 5-居家安全与应急 6-精神慰藉与社交")
    private Integer skillType;

    @ApiModelProperty("技能详细描述")
    private String description;

    @ApiModelProperty("护工添加该技能时是否需审核：0-否(默认通过) 1-是")
    private Integer needAudit;

    @ApiModelProperty("创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @ApiModelProperty("更新时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
