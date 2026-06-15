package com.caregiver.carelink.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.math.BigDecimal;

/**
 * AI聊天请求DTO
 *
 * @author CareLink
 * @since 2026-02-11
 */
@Data
@ApiModel(description = "AI聊天请求")
public class ChatRequestDTO {

    @ApiModelProperty(value = "用户消息内容", required = true)
    @NotBlank(message = "消息内容不能为空")
    private String message;

    @ApiModelProperty("会话ID（不传则使用默认会话）")
    private String conversationId;

    @ApiModelProperty(value = "对话场景标识（可选）。传 'care_recommend' 表示进入智能护理方案推荐对话，AI将主动引导用户描述需求", example = "care_recommend")
    private String scene;

    @ApiModelProperty(value = "用户当前经度（可选，前端自动获取）", example = "116.397128")
    private BigDecimal longitude;

    @ApiModelProperty(value = "用户当前纬度（可选，前端自动获取）", example = "39.916527")
    private BigDecimal latitude;

    @ApiModelProperty(value = "用户当前城市编码（可选，前端自动获取）", example = "156110100")
    private String cityCode;
}
