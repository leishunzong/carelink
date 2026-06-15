package com.caregiver.carelink.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 评价详情VO
 *
 * @author CareLink
 * @since 2026-01-29
 */
@Data
@ApiModel(description = "评价详情")
public class ReviewVO {

    @ApiModelProperty("评价ID")
    private Long id;

    @ApiModelProperty("订单号")
    private String orderNo;

    @ApiModelProperty("被评价护工ID")
    private Long caregiverId;

    @ApiModelProperty("被评价护工姓名")
    private String caregiverName;

    @ApiModelProperty("服务时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime serviceDate;

    @ApiModelProperty("评价人昵称（匿名时显示'匿名用户'）")
    private String nickname;

    @ApiModelProperty("评价人头像（匿名时为null）")
    private String avatar;

    @ApiModelProperty("评价内容")
    private String content;

    @ApiModelProperty("评价类型：1-好评, 2-差评")
    private Integer type;

    @ApiModelProperty("星级评分：1-5星")
    private Integer stars;

    @ApiModelProperty("是否匿名")
    private Integer isAnonymous;

    @ApiModelProperty("评价标签列表")
    private List<String> tags;

    @ApiModelProperty("创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
