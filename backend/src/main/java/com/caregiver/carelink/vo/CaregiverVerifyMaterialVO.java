package com.caregiver.carelink.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 护工审核材料 VO
 *
 * @author CareLink
 * @since 2026-02-24
 */
@Data
@ApiModel(description = "护工审核材料")
public class CaregiverVerifyMaterialVO {

    @ApiModelProperty("材料类型：1-身份证正面 2-身份证反面 3-护工资格证 4-其他证明材料")
    private Integer materialType;

    @ApiModelProperty("材料类型描述")
    private String materialTypeName;

    @ApiModelProperty("文件URL")
    private String fileUrl;

    @ApiModelProperty("排序")
    private Integer sortOrder;
}
