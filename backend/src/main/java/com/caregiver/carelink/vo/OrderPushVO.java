package com.caregiver.carelink.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 推送给护工的新订单信息（WebSocket），供护工判断是否接单
 *
 * @author CareLink
 * @since 2026-02-11
 */
@Data
@ApiModel(description = "推送订单信息（护工抢单用）")
public class OrderPushVO {

    // ---------- 订单基本信息 ----------
    @ApiModelProperty("订单ID")
    private Long orderId;
    @ApiModelProperty("订单号")
    private String orderNo;
    @ApiModelProperty("订单类型：1-系统匹配 2-定向预约（前端可据此渲染不同按钮）")
    private Integer orderType;
    @ApiModelProperty("订单状态：2-待接单")
    private Integer status;
    @ApiModelProperty("下单备注/注意事项")
    private String specialRemark;

    // ---------- 下单人（联系人） ----------
    @ApiModelProperty("联系人姓名")
    private String contactName;
    @ApiModelProperty("联系人电话")
    private String contactPhone;

    // ---------- 服务包与费用 ----------
    @ApiModelProperty("服务包ID")
    private Long packageId;
    @ApiModelProperty("服务包名称")
    private String packageName;
    @ApiModelProperty("计费方式：1-按月 2-按天 3-按小时 4-按次")
    private Integer billingMethod;
    @ApiModelProperty("单价")
    private BigDecimal unitPrice;
    @ApiModelProperty("购买数量")
    private Integer buyQuantity;
    @ApiModelProperty("总费用")
    private BigDecimal totalAmount;

    // ---------- 服务对象（老人）基本情况 ----------
    @ApiModelProperty("服务对象姓名")
    private String clientName;
    @ApiModelProperty("性别：0未知 1男 2女")
    private Integer clientGender;
    @ApiModelProperty("年龄")
    private Integer clientAge;
    @ApiModelProperty("身高(cm)")
    private BigDecimal clientHeight;
    @ApiModelProperty("体重(kg)")
    private BigDecimal clientWeight;
    @ApiModelProperty("智力情况")
    private String intellectStatus;
    @ApiModelProperty("自理能力")
    private String selfCareAbility;
    @ApiModelProperty("病史")
    private String medicalHistory;
    @ApiModelProperty("备注")
    private String remarks;

    // ---------- 服务地址 ----------
    @ApiModelProperty("地址")
    private String address;
    @ApiModelProperty("门牌号")
    private String doorNumber;
    @ApiModelProperty("详细地址（地址+门牌号）")
    private String detailAddress;

    // ---------- 时间 ----------
    @ApiModelProperty("预约上门时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime expectStartTime;
}
