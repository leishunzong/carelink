package com.caregiver.carelink.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 护工信息VO
 *
 * @author CareLink
 * @since 2026-01-29
 */
@Data
@ApiModel(description = "护工信息")
public class CaregiverInfoVO {

    @ApiModelProperty("护工ID")
    private Long id;

    @ApiModelProperty("用户名")
    private String username;

    @ApiModelProperty("手机号")
    private String phone;

    @ApiModelProperty("真实姓名")
    private String realName;

    @ApiModelProperty("头像")
    private String avatar;

    @ApiModelProperty("性别: 1男 2女")
    private Integer gender;

    @ApiModelProperty("出生日期")
    @JsonFormat(pattern = "yyyy-MM-dd")
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

    @ApiModelProperty("审核状态: 0待审 1通过 2拒绝")
    private Integer verifyStatus;

    @ApiModelProperty("工作状态: 1接单中 2服务中 3休息中")
    private Integer workState;

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

    @ApiModelProperty("创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
