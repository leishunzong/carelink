package com.caregiver.carelink.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 护工入驻申请列表项（管理员端）
 *
 * @author CareLink
 * @since 2026-02-24
 */
@Data
@ApiModel(description = "护工入驻申请")
public class CaregiverSettleApplyVO {

    @ApiModelProperty("护工ID")
    private Long id;

    @ApiModelProperty("姓名")
    private String realName;

    @ApiModelProperty("手机号")
    private String phone;

    @ApiModelProperty("头像URL")
    private String avatar;

    @ApiModelProperty("性别: 1男 2女")
    private Integer gender;

    @ApiModelProperty("出生日期")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate birthday;

    @ApiModelProperty("民族")
    private String ethnicity;

    @ApiModelProperty("星座")
    private String zodiac;

    @ApiModelProperty("籍贯")
    private String nativePlace;

    @ApiModelProperty("学历")
    private String education;

    @ApiModelProperty("从业年限（年）")
    private Integer workYears;

    @ApiModelProperty("服务城市名称")
    private String cityName;

    @ApiModelProperty("居住地址")
    private String residentAddress;

    @ApiModelProperty("审核状态: 0待审 1通过 2拒绝")
    private Integer verifyStatus;

    @ApiModelProperty("创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @ApiModelProperty("更新时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;

    @ApiModelProperty("审核材料列表")
    private List<CaregiverVerifyMaterialVO> verifyMaterials;
}
