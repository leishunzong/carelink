package com.caregiver.carelink.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 服务地址实体类（联系人、手机号、地址、门牌号、经纬度、是否默认）
 *
 * @author CareLink
 * @since 2026-01-29
 */
@Data
@TableName("service_address")
@ApiModel(description = "服务地址实体")
public class ServiceAddress implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty("主键ID")
    @TableId(type = IdType.AUTO)
    private Long id;

    @ApiModelProperty("用户ID")
    private Long userId;

    @ApiModelProperty("联系人")
    private String contactName;

    @ApiModelProperty("手机号")
    private String contactPhone;

    @ApiModelProperty("地址（地图选址或手动输入）")
    private String address;

    @ApiModelProperty("门牌号，例：10号楼6单元1001室")
    private String doorNumber;

    @ApiModelProperty("经度")
    private BigDecimal longitude;

    @ApiModelProperty("纬度")
    private BigDecimal latitude;

    @ApiModelProperty("是否默认地址: 1是 0否")
    private Integer isDefault;

    @ApiModelProperty("创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @ApiModelProperty("更新时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
