package com.caregiver.carelink.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 护工审核材料（身份证件、资格证、其他证明材料）
 *
 * @author CareLink
 * @since 2026-02-24
 */
@Data
@TableName("caregiver_verify_material")
@ApiModel(description = "护工审核材料")
public class CaregiverVerifyMaterial implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    @ApiModelProperty("护工ID")
    private Long caregiverId;

    /** 材料类型：1-身份证正面 2-身份证反面 3-护工资格证 4-其他证明材料 */
    @ApiModelProperty("材料类型：1-身份证正面 2-身份证反面 3-护工资格证 4-其他证明材料")
    private Integer materialType;

    @ApiModelProperty("文件URL（上传后返回的地址）")
    private String fileUrl;

    @ApiModelProperty("排序（同类型多张时使用，如其他证明材料）")
    private Integer sortOrder;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
