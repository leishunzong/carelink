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
 * 护工评价实体类
 *
 * @author CareLink
 * @since 2026-01-29
 */
@Data
@TableName("review")
@ApiModel(description = "护工评价实体")
public class Review implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty("评价ID")
    @TableId(type = IdType.AUTO)
    private Long id;

    @ApiModelProperty("订单ID")
    private Long orderId;

    @ApiModelProperty("订单号快照")
    private String orderNo;

    @ApiModelProperty("评价人ID")
    private Long userId;

    @ApiModelProperty("评价人昵称快照")
    private String nickname;

    @ApiModelProperty("评价人头像快照")
    private String avatar;

    @ApiModelProperty("被评价护工ID")
    private Long caregiverId;

    @ApiModelProperty("服务时间快照")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime serviceDate;

    @ApiModelProperty("评价内容")
    private String content;

    @ApiModelProperty("评价类型：1-好评, 2-差评")
    private Integer type;

    @ApiModelProperty("星级评分：1-5星")
    private Integer stars;

    @ApiModelProperty("是否匿名：0-否, 1-是")
    private Integer isAnonymous;

    @ApiModelProperty("创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @ApiModelProperty("更新时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
