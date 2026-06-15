package com.caregiver.carelink.store;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingStore;

/**
 * 支持按文档 ID 删除的向量库（RAG 单文档删除时只移除该文档的向量，无需全量重建）
 *
 * @author CareLink
 * @since 2026-02-26
 */
public interface DocumentAwareEmbeddingStore extends EmbeddingStore<TextSegment> {

    /**
     * 移除指定文档 ID 对应的所有向量
     *
     * @param documentId RAG 文档主键 ID
     */
    void removeByDocumentId(Long documentId);
}
