package com.caregiver.carelink.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 订单推送记录：记录某订单曾推送给哪些护工，重试匹配时排除已推送过的护工
 *
 * @author CareLink
 * @since 2026-02-11
 */
@Data
@TableName("order_push_record")
@ApiModel(description = "订单推送记录")
public class OrderPushRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    @ApiModelProperty("订单ID")
    private Long orderId;
    @ApiModelProperty("被推送的护工ID")
    private Long caregiverId;
    @ApiModelProperty("推送时间")
    private LocalDateTime createTime;
}
