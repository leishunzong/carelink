package com.caregiver.carelink.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单列表项 VO（用户/护工订单列表卡片展示用）
 *
 * @author CareLink
 * @since 2026-02-28
 */
@Data
@ApiModel(description = "订单列表项")
public class OrderListItemVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty("订单ID")
    private Long id;

    @ApiModelProperty("订单号")
    private String orderNo;

    @ApiModelProperty("订单类型：1-系统匹配 2-定向预约")
    private Integer orderType;

    @ApiModelProperty("订单状态：1-待支付 2-待接单 3-待上门 4-服务中 5-待确认 6-已完成 7-已取消 8-已关闭")
    private Integer status;

    @ApiModelProperty("服务包ID（隐藏字段）")
    private Long packageId;

    @ApiModelProperty("服务包名称")
    private String packageName;

    @ApiModelProperty("护工姓名（已派单时有值）")
    private String caregiverName;

    @ApiModelProperty("地址")
    private String address;
    @ApiModelProperty("门牌号")
    private String doorNumber;
    @ApiModelProperty("服务详细地址（地址+门牌号）")
    private String detailAddress;

    @ApiModelProperty("计费方式：1-按月 2-按天 3-按小时 4-按次")
    private Integer billingMethod;

    @ApiModelProperty("单价")
    private BigDecimal unitPrice;

    @ApiModelProperty("购买数量")
    private Integer buyQuantity;

    @ApiModelProperty("总费用")
    private BigDecimal totalAmount;

    // ========== 服务对象信息 ==========

    @ApiModelProperty("服务对象姓名")
    private String clientName;

    @ApiModelProperty("服务对象性别：1-男 2-女")
    private Integer clientGender;

    @ApiModelProperty("服务对象年龄")
    private Integer clientAge;

    @ApiModelProperty("自理能力")
    private String selfCareAbility;

    @ApiModelProperty("病史")
    private String medicalHistory;

    // ========== 联系人信息 ==========

    @ApiModelProperty("联系人姓名")
    private String contactName;

    @ApiModelProperty("联系人电话")
    private String contactPhone;

    // ========== 匹配要求（系统匹配订单） ==========

    @ApiModelProperty("匹配半径（km）")
    private Integer matchingRadius;

    @ApiModelProperty("要求性别：1-男 2-女")
    private Integer reqGender;

    @ApiModelProperty("要求工作年限")
    private Integer reqWorkYears;

    @ApiModelProperty("要求籍贯")
    private String reqNativePlace;

    @ApiModelProperty("特殊备注")
    private String specialRemark;

    @ApiModelProperty("预约上门时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime expectStartTime;

    @ApiModelProperty("下单时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}

