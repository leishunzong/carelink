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

@Data
@TableName("ai_message")
@ApiModel("AI 会话消息")
public class AiMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    @ApiModelProperty("主键ID")
    private Long id;

    @ApiModelProperty("会话ID")
    private Long conversationId;

    @ApiModelProperty("用户ID")
    private Long userId;

    @ApiModelProperty("角色：1-用户 2-AI 3-系统")
    private Integer role;

    @ApiModelProperty("消息内容")
    private String content;

    @ApiModelProperty("会话内顺序号，从1开始递增")
    private Integer seq;

    @ApiModelProperty("创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}

