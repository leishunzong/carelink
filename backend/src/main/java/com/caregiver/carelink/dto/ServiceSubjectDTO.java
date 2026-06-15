package com.caregiver.carelink.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 服务对象DTO
 *
 * @author CareLink
 * @since 2026-01-29
 */
@Data
@ApiModel(description = "服务对象信息")
public class ServiceSubjectDTO {

    @ApiModelProperty(value = "姓名", required = true)
    private String name;

    @ApiModelProperty(value = "关系", required = true)
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
}
