package com.caregiver.carelink.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.math.BigDecimal;

/**
 * 服务地址 DTO（联系人、手机号、地址、门牌号、经纬度、是否默认）
 *
 * @author CareLink
 * @since 2026-01-29
 */
@Data
@ApiModel(description = "服务地址信息")
public class ServiceAddressDTO {

    @ApiModelProperty(value = "联系人", required = true)
    @NotBlank(message = "请填写联系人")
    private String contactName;

    @ApiModelProperty(value = "手机号", required = true)
    @NotBlank(message = "请填写手机号")
    private String contactPhone;

    @ApiModelProperty(value = "地址（地图选址或手动输入）", required = true)
    @NotBlank(message = "请填写或选择地址")
    private String address;

    @ApiModelProperty("门牌号，例：10号楼6单元1001室")
    private String doorNumber;

    @ApiModelProperty("经度")
    private BigDecimal longitude;

    @ApiModelProperty("纬度")
    private BigDecimal latitude;

    @ApiModelProperty("是否默认地址: 1是 0否")
    private Integer isDefault;
}
