package com.caregiver.carelink.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@ApiModel("AI 会话消息")
public class AiMessageVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty("消息ID")
    private Long id;

    @ApiModelProperty("角色：1-用户 2-AI 3-系统")
    private Integer role;

    @ApiModelProperty("消息内容")
    private String content;

    @ApiModelProperty("会话内顺序号")
    private Integer seq;

    @ApiModelProperty("创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}

