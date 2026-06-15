package com.caregiver.carelink.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.caregiver.carelink.entity.RagDocument;

import java.util.List;

/**
 * RAG 知识库文档服务（管理员上传、列表、删除；向量库重建）
 *
 * @author CareLink
 * @since 2026-02-24
 */
public interface RagDocumentService extends IService<RagDocument> {

    /**
     * 管理员上传文档：落库并加入向量库（切片+嵌入+add）
     */
    RagDocument addDocument(String title, String fileName, String content);

    /**
     * 列表（按创建时间倒序）
     */
    List<RagDocument> listDocuments();

    /**
     * 删除文档并重建向量库（从 DB 全量重新加载）
     */
    void deleteDocumentAndRebuild(Long id);

    /**
     * 从 DB 全量加载到向量库（启动时或删除后调用）
     */
    void loadAllIntoStore();
}
