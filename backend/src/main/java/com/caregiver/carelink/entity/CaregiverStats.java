package com.caregiver.carelink.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 护工绩效统计实体类
 *
 * @author CareLink
 * @since 2026-01-29
 */
@Data
@TableName("caregiver_stats")
@ApiModel(description = "护工绩效统计实体")
public class CaregiverStats implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty("主键ID")
    @TableId(type = IdType.AUTO)
    private Long id;

    @ApiModelProperty("护工ID")
    private Long caregiverId;

    @ApiModelProperty("累计完成订单数")
    private Integer orderCount;

    @ApiModelProperty("累计评价数")
    private Integer reviewCount;

    @ApiModelProperty("爽约/取消单数")
    private Integer cancelCount;

    @ApiModelProperty("好评率")
    private BigDecimal goodReviewRate;

    @ApiModelProperty("好评个数")
    private Integer starCount;

    @ApiModelProperty("星级总和(1-5星)，用于计算平均分")
    private Integer starRatingSum;

    @ApiModelProperty("更新时间")
    private LocalDateTime updateTime;
}
