package com.caregiver.carelink.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 护工「我开通的服务包」列表项（仅基本信息 + 准入时间）
 *
 * @author CareLink
 * @since 2026-02-28
 */
@Data
@ApiModel(description = "护工已开通服务包列表项（基本信息+准入时间）")
public class CaregiverMyPackageVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty("服务包ID")
    private Long id;

    @ApiModelProperty("服务包名称")
    private String name;

    @ApiModelProperty("服务类型：1-居家陪护 2-医院陪护 3-周期护理 4-家政服务 5-陪诊服务 6-母婴护理")
    private Integer category;

    @ApiModelProperty("服务包封面图片URL")
    private String coverImage;

    @ApiModelProperty("服务包简介")
    private String description;

    @ApiModelProperty("准入时间（开通时间）")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime bindTime;
}
