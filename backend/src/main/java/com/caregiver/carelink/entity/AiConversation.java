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
@TableName("ai_conversation")
@ApiModel("AI 会话")
public class AiConversation implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    @ApiModelProperty("主键ID")
    private Long id;

    @ApiModelProperty("用户ID")
    private Long userId;

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

    @ApiModelProperty("状态：1-正常 0-已删除")
    private Integer status;

    @ApiModelProperty("创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @ApiModelProperty("更新时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}

