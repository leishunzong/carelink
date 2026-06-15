package com.caregiver.carelink.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
@ApiModel(description = "附近护工信息")
public class NearbyCaregiverVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty("护工ID")
    private Long id;

    @ApiModelProperty("真实姓名")
    private String realName;

    @ApiModelProperty("头像")
    private String avatar;

    @ApiModelProperty("从业年限")
    private Integer workYears;

    @ApiModelProperty("距离用户的距离，单位公里")
    private Double distanceKm;

    @ApiModelProperty("完成订单数")
    private Integer orderCount;

    @ApiModelProperty("好评率")
    private BigDecimal goodReviewRate;

    @ApiModelProperty("平均星级（1-5星，保留1位小数）")
    private BigDecimal averageRating;
}

