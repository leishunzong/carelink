package com.caregiver.carelink.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 标签统计VO
 *
 * @author CareLink
 * @since 2026-01-29
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ApiModel(description = "标签统计")
public class TagCountVO {

    @ApiModelProperty("标签ID")
    private Long tagId;

    @ApiModelProperty("标签名称")
    private String tagName;

    @ApiModelProperty("标签类型：1-好评, 2-差评")
    private Integer tagType;

    @ApiModelProperty("被评价次数")
    private Integer count;
}
