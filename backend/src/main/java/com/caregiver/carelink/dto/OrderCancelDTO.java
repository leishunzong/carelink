package com.caregiver.carelink.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.Size;

/**
 * 用户取消服务包订单请求
 *
 * @author CareLink
 * @since 2026-02-11
 */
@Data
@ApiModel(description = "取消订单参数")
public class OrderCancelDTO {

    @ApiModelProperty("取消原因（选填）")
    @Size(max = 255)
    private String cancelReason;
}
