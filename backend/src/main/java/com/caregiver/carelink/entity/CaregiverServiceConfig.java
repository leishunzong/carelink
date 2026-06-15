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
 * 护工服务包准入关联实体
 *
 * @author CareLink
 * @since 2026-02-11
 */
@Data
@TableName("caregiver_service_config")
@ApiModel(description = "护工服务包准入关联")
public class CaregiverServiceConfig implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty("主键ID")
    @TableId(type = IdType.AUTO)
    private Long id;

    @ApiModelProperty("护工ID")
    private Long caregiverId;

    @ApiModelProperty("系统服务包ID")
    private Long packageId;

    @ApiModelProperty("获得准入的时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
