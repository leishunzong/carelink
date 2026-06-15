package com.caregiver.carelink.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 核心服务订单实体（表名 order，无前缀）
 *
 * @author CareLink
 * @since 2026-02-11
 */
@Data
@TableName("`order`")
@ApiModel(description = "服务订单")
public class Order implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    @ApiModelProperty("业务订单号")
    private String orderNo;
    @ApiModelProperty("1-系统匹配 2-定向预约")
    private Integer orderType;
    @ApiModelProperty("1-待支付 2-待接单 3-待上门 4-服务中 5-待确认 6-已完成 7-已取消 8-已关闭")
    private Integer status;

    private Long userId;
    private Long caregiverId;

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

    private String address;
    private String doorNumber;
    private String detailAddress;
    private BigDecimal longitude;
    private BigDecimal latitude;
    @ApiModelProperty("服务城市编码，用于匹配派单")
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
