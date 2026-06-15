package com.caregiver.carelink.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 护工信息更新DTO
 *
 * @author CareLink
 * @since 2026-01-29
 */
@Data
@ApiModel(description = "护工信息更新请求")
public class CaregiverUpdateDTO {

    @ApiModelProperty("头像")
    private String avatar;

    @ApiModelProperty("性别: 1男 2女")
    private Integer gender;

    @ApiModelProperty("出生日期")
    private LocalDate birthday;

    @ApiModelProperty("籍贯")
    private String nativePlace;

    @ApiModelProperty("学历")
    private String education;

    @ApiModelProperty("民族")
    private String ethnicity;

    @ApiModelProperty("生肖/星座")
    private String zodiac;

    @ApiModelProperty("从业年限")
    private Integer workYears;

    @ApiModelProperty("服务城市编码")
    private String cityCode;

    @ApiModelProperty("服务城市名称")
    private String cityName;

    @ApiModelProperty("常驻地址")
    private String residentAddress;

    @ApiModelProperty("经度")
    private BigDecimal longitude;

    @ApiModelProperty("纬度")
    private BigDecimal latitude;
}
