package com.caregiver.carelink.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotNull;

/**
 * 订单支付（模拟）请求
 *
 * @author CareLink
 * @since 2026-02-11
 */
@Data
@ApiModel(description = "订单支付")
public class OrderPayDTO {

    @NotNull(message = "订单ID不能为空")
    @ApiModelProperty(value = "订单ID", required = true)
    private Long orderId;
}
