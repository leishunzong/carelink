package com.caregiver.carelink.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 护工统计信息VO
 *
 * @author CareLink
 * @since 2026-01-29
 */
@Data
@ApiModel(description = "护工统计信息")
public class CaregiverStatsVO {

    @ApiModelProperty("护工ID")
    private Long caregiverId;

    @ApiModelProperty("完成订单数")
    private Integer orderCount;

    @ApiModelProperty("累计评价数")
    private Integer reviewCount;

    @ApiModelProperty("好评数")
    private Integer starCount;

    @ApiModelProperty("好评率")
    private BigDecimal goodReviewRate;

    @ApiModelProperty("平均星级（1-5星，保留1位小数）")
    private BigDecimal averageRating;

    @ApiModelProperty("取消单数")
    private Integer cancelCount;

    @ApiModelProperty("评价标签统计（带次数）")
    private List<TagCountVO> tagStats;
}
