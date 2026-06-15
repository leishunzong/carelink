package com.caregiver.carelink.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * RAG 知识库文档列表项（管理端可返回正文便于查看/编辑）
 *
 * @author CareLink
 * @since 2026-02-24
 */
@Data
@ApiModel(description = "RAG知识库文档列表项")
public class RagDocumentVO {

    @ApiModelProperty("文档ID")
    private Long id;

    @ApiModelProperty("文档标题")
    private String title;

    @ApiModelProperty("原始文件名")
    private String fileName;

    @ApiModelProperty("正文内容")
    private String content;

    @ApiModelProperty("创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @ApiModelProperty("更新时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
