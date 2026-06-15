package com.caregiver.carelink.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 护工入驻请求 DTO（补齐基础信息 + 审核材料：身份证件、资格证、其他证明材料）
 *
 * @author CareLink
 * @since 2026-02-24
 */
@Data
@ApiModel(description = "护工入驻请求（补齐姓名、头像、性别等基本信息并提交审核材料）")
public class CaregiverSettleDTO {

    // ---------- 基本信息（由前端传入，入驻时补全） ----------
    @ApiModelProperty(value = "真实姓名", required = true)
    @NotBlank(message = "真实姓名不能为空")
    private String realName;

    @ApiModelProperty("头像（实拍照正面免冠素颜照片）")
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

    // ---------- 审核材料（先通过 /file/upload/image 上传获取 url 再传入） ----------
    @ApiModelProperty("身份证正面图片URL")
    private String idCardFrontUrl;

    @ApiModelProperty("身份证反面图片URL")
    private String idCardBackUrl;

    @ApiModelProperty("护工资格证图片URL")
    private String qualificationCertUrl;

    @ApiModelProperty("其他证明材料图片URL列表（可多张）")
    private List<String> otherMaterialUrls;
}
