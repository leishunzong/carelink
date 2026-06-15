package com.caregiver.carelink.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 服务包订单详情（用户/护工查询）
 *
 * @author CareLink
 * @since 2026-02-11
 */
@Data
@ApiModel(description = "服务包订单详情")
public class OrderDetailVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String orderNo;
    private Integer orderType;
    private Integer status;

    private Long userId;
    private Long caregiverId;
    private String caregiverName;
    private String caregiverPhone;
    @ApiModelProperty("护工头像URL，前端展示用")
    private String caregiverAvatar;

    private String contactName;
    private String contactPhone;

    private String clientName;
    private Integer clientGender;
    private Integer clientAge;
    private BigDecimal clientHeight;
    private BigDecimal clientWeight;
    private String intellectStatus;
    private String selfCareAbility;
    private String medicalHistory;
    private String remarks;

    @ApiModelProperty("地址")
    private String address;
    @ApiModelProperty("门牌号")
    private String doorNumber;
    @ApiModelProperty("详细地址快照（地址+门牌号）")
    private String detailAddress;
    private BigDecimal longitude;
    private BigDecimal latitude;
    private String cityCode;
    private Integer matchingRadius;

    private Long packageId;
    private String packageName;
    private Integer billingMethod;
    private BigDecimal unitPrice;
    private Integer buyQuantity;
    private BigDecimal totalAmount;

    private Integer reqGender;
    private Integer reqWorkYears;
    private String reqNativePlace;
    private String specialRemark;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime expectStartTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime realStartTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime finishTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime cancelTime;
    private String cancelReason;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
