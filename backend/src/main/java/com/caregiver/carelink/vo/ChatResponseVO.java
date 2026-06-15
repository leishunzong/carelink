package com.caregiver.carelink.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * AI聊天响应VO
 *
 * @author CareLink
 * @since 2026-02-11
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ApiModel(description = "AI聊天响应")
public class ChatResponseVO {

    @ApiModelProperty("AI回复内容")
    private String reply;

    @ApiModelProperty("会话ID")
    private String conversationId;
}
