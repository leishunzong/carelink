package com.caregiver.carelink.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 护工详情聚合VO（用户查看护工详情用）
 *
 * @author CareLink
 * @since 2026-02-28
 */
@Data
@ApiModel(description = "护工详情聚合信息")
public class CaregiverDetailVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty("护工基础信息")
    private CaregiverInfoVO basicInfo;

    @ApiModelProperty("护工技能列表")
    private List<CaregiverSkillVO> skills;

    @ApiModelProperty("护工可提供的服务包列表（仅上架）")
    private List<ServicePackageVO> packages;

    @ApiModelProperty("护工统计信息（订单数、评价数、好评率、标签统计等）")
    private CaregiverStatsVO stats;
}

