package com.caregiver.carelink.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 管理端订单列表项 VO（在列表项基础上增加：用户昵称、开始时间、结束时间、上门时间）
 *
 * @author CareLink
 * @since 2026-02-26
 */
@Data
@ApiModel(description = "管理端订单列表项")
public class AdminOrderListItemVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty("订单ID")
    private Long id;

    @ApiModelProperty("订单号")
    private String orderNo;

    @ApiModelProperty("订单类型：1-系统匹配 2-定向预约")
    private Integer orderType;

    @ApiModelProperty("订单状态：1-待支付 2-待接单 3-待上门 4-服务中 5-待确认 6-已完成 7-已取消 8-已关闭")
    private Integer status;

    @ApiModelProperty("服务包ID")
    private Long packageId;

    @ApiModelProperty("服务包名称")
    private String packageName;

    @ApiModelProperty("护工姓名（已派单时有值）")
    private String caregiverName;

    @ApiModelProperty("下单用户昵称")
    private String nickname;

    @ApiModelProperty("地址")
    private String address;
    @ApiModelProperty("门牌号")
    private String doorNumber;
    @ApiModelProperty("服务详细地址（地址+门牌号）")
    private String detailAddress;

    @ApiModelProperty("计费方式：1-按月 2-按天 3-按小时 4-按次")
    private Integer billingMethod;

    @ApiModelProperty("购买数量")
    private Integer buyQuantity;

    @ApiModelProperty("单价")
    private BigDecimal unitPrice;

    @ApiModelProperty("总费用")
    private BigDecimal totalAmount;

    @ApiModelProperty("预约上门时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime expectStartTime;

    @ApiModelProperty("实际上门/开始时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime realStartTime;

    @ApiModelProperty("结束时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime finishTime;

    @ApiModelProperty("下单时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
