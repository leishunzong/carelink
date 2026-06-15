package com.caregiver.carelink.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 管理端统计数据 VO
 *
 * @author CareLink
 * @since 2026-02-26
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ApiModel(description = "管理端统计数据")
public class AdminStatsVO {

    @ApiModelProperty("护工总数")
    private Long caregiverTotal;

    @ApiModelProperty("订单总数")
    private Long orderTotal;

    @ApiModelProperty("待审核的护工入驻总数")
    private Long pendingSettleTotal;

    @ApiModelProperty("待审核的技能总数")
    private Long pendingSkillTotal;

    @ApiModelProperty("用户总数")
    private Long userTotal;

    @ApiModelProperty("评价总数")
    private Long reviewTotal;

    @ApiModelProperty("服务包总数")
    private Long servicePackageTotal;

    @ApiModelProperty("技能总数")
    private Long skillTotal;

    @ApiModelProperty("评价标签总数")
    private Long reviewTagTotal;

    @ApiModelProperty("知识库总数")
    private Long ragDocumentTotal;

    @ApiModelProperty("当日营业额（元），今日已完成订单金额合计")
    private BigDecimal todayRevenue;

    @ApiModelProperty("总营业额（元），历史已完成订单金额合计")
    private BigDecimal totalRevenue;
}
