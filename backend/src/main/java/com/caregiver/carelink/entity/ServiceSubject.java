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
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 服务对象实体类
 *
 * @author CareLink
 * @since 2026-01-29
 */
@Data
@TableName("service_subject")
@ApiModel(description = "服务对象实体")
public class ServiceSubject implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty("主键ID")
    @TableId(type = IdType.AUTO)
    private Long id;

    @ApiModelProperty("用户ID")
    private Long userId;

    @ApiModelProperty("姓名")
    private String name;

    @ApiModelProperty("关系: 如父子、母子、本人等")
    private String relationship;

    @ApiModelProperty("出生日期")
    private LocalDate birthday;

    @ApiModelProperty("性别: 0未知 1男 2女")
    private Integer gender;

    @ApiModelProperty("身高(cm)")
    private BigDecimal height;

    @ApiModelProperty("体重(kg)")
    private BigDecimal weight;

    @ApiModelProperty("智力情况")
    private String intellectStatus;

    @ApiModelProperty("自理能力")
    private String selfCareAbility;

    @ApiModelProperty("病史(多选标签，逗号分隔)")
    private String medicalHistory;

    @ApiModelProperty("注意事项/备注")
    private String remarks;

    @ApiModelProperty("是否默认: 1是 0否")
    private Integer isDefault;

    @ApiModelProperty("创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @ApiModelProperty("更新时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
