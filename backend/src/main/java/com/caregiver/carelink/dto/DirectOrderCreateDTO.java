package com.caregiver.carelink.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 用户创建定向预约订单（指定护工、选服务包）请求 DTO。
 * 与匹配订单相比：必传护工ID，无需匹配半径与护工要求。
 *
 * @author CareLink
 * @since 2026-02-11
 */
@Data
@ApiModel(description = "创建定向预约订单参数")
public class DirectOrderCreateDTO {

    @NotNull(message = "护工ID不能为空")
    @ApiModelProperty(value = "指定护工ID", required = true)
    private Long caregiverId;

    @NotBlank(message = "联系人姓名不能为空")
    @ApiModelProperty(value = "联系人姓名", required = true)
    private String contactName;

    @NotBlank(message = "联系人电话不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    @ApiModelProperty(value = "联系人电话", required = true)
    private String contactPhone;

    @NotBlank(message = "服务对象姓名不能为空")
    @ApiModelProperty(value = "老人/服务对象姓名", required = true)
    private String clientName;

    @ApiModelProperty("服务对象性别: 0未知 1男 2女")
    private Integer clientGender;
    @ApiModelProperty("服务对象年龄")
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

    @NotBlank(message = "地址不能为空")
    @ApiModelProperty(value = "地址（地图选址或手动输入）", required = true)
    private String address;

    @ApiModelProperty("门牌号，例：10号楼6单元1001室")
    private String doorNumber;

    @NotNull(message = "经度不能为空")
    @ApiModelProperty(value = "经度", required = true)
    private BigDecimal longitude;

    @NotNull(message = "纬度不能为空")
    @ApiModelProperty(value = "纬度", required = true)
    private BigDecimal latitude;

    @ApiModelProperty("服务城市编码（不传则从用户表补全）")
    private String cityCode;

    @NotNull(message = "服务包ID不能为空")
    @ApiModelProperty(value = "服务包ID", required = true)
    private Long packageId;

    @NotBlank(message = "服务包名称不能为空")
    @ApiModelProperty(value = "服务包名称快照", required = true)
    private String packageName;

    @NotNull(message = "计费方式不能为空")
    @ApiModelProperty(value = "1-按月 2-按天 3-按小时 4-按次", required = true)
    private Integer billingMethod;

    @NotNull(message = "单价不能为空")
    @ApiModelProperty(value = "成交单价", required = true)
    private BigDecimal unitPrice;

    @NotNull(message = "购买数量不能为空")
    @Min(1)
    @ApiModelProperty(value = "购买数量", required = true)
    private Integer buyQuantity;

    @NotNull(message = "总费用不能为空")
    @ApiModelProperty(value = "总费用", required = true)
    private BigDecimal totalAmount;

    @ApiModelProperty("注意事项/备注")
    @Size(max = 500)
    private String specialRemark;

    @NotNull(message = "预约上门时间不能为空")
    @ApiModelProperty(value = "预约上门时间", required = true)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime expectStartTime;
}
