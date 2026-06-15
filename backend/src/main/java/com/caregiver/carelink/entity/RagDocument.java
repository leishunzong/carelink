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

/**
 * RAG 知识库文档（管理员上传，用于检索增强）
 *
 * @author CareLink
 * @since 2026-02-24
 */
@Data
@TableName("rag_document")
@ApiModel(description = "RAG知识库文档")
public class RagDocument implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    @ApiModelProperty("文档标题（展示用）")
    private String title;

    @ApiModelProperty("原始文件名")
    private String fileName;

    @ApiModelProperty("正文内容（用于切片与向量化）")
    private String content;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
