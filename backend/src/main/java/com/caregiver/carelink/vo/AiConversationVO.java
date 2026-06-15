package com.caregiver.carelink.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@ApiModel("AI 会话列表项")
public class AiConversationVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty("会话内部ID")
    private Long id;

    @ApiModelProperty("前端会话ID")
    private String conversationId;

    @ApiModelProperty("会话标题")
    private String title;

    @ApiModelProperty("最后一条用户问题")
    private String lastQuestion;

    @ApiModelProperty("最后一次AI回复（摘要）")
    private String lastAnswer;

    @ApiModelProperty("消息总数")
    private Integer messageCount;

    @ApiModelProperty("是否置顶：1-是 0-否")
    private Integer isPinned;

    @ApiModelProperty("是否收藏：1-是 0-否")
    private Integer isFavorite;

    @ApiModelProperty("最后更新时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}

