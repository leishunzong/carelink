package com.caregiver.carelink.store;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.EmbeddingStore;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 可重建的向量库包装
 * <p>
 * 委托给内部 EmbeddingStore；支持运行时替换内部 store，用于「从 DB 全量重建」或管理员上传后增量加入。
 *
 * @author CareLink
 * @since 2026-02-24
 */
public class RebuildableEmbeddingStore implements EmbeddingStore<TextSegment> {

    private final AtomicReference<EmbeddingStore<TextSegment>> delegate;

    public RebuildableEmbeddingStore(EmbeddingStore<TextSegment> initial) {
        this.delegate = new AtomicReference<>(initial);
    }

    @Override
    public String add(Embedding embedding) {
        return delegate.get().add(embedding);
    }

    @Override
    public void add(String id, Embedding embedding) {
        delegate.get().add(id, embedding);
    }

    @Override
    public String add(Embedding embedding, TextSegment textSegment) {
        return delegate.get().add(embedding, textSegment);
    }

    @Override
    public List<String> addAll(List<Embedding> embeddings) {
        return delegate.get().addAll(embeddings);
    }

    @Override
    public List<String> addAll(List<Embedding> embeddings, List<TextSegment> segments) {
        return delegate.get().addAll(embeddings, segments);
    }

    @Override
    public void addAll(List<String> ids, List<Embedding> embeddings, List<TextSegment> segments) {
        delegate.get().addAll(ids, embeddings, segments);
    }

    @Override
    public EmbeddingSearchResult<TextSegment> search(EmbeddingSearchRequest request) {
        return delegate.get().search(request);
    }

    /**
     * 替换内部向量库（用于从 DB 全量重建后替换）
     */
    public void replaceDelegate(EmbeddingStore<TextSegment> newStore) {
        this.delegate.set(newStore);
    }

    /**
     * 按文档 ID 移除该文档的所有向量（仅当内部 store 为 DocumentAwareEmbeddingStore 时有效，避免全量重建）
     */
    public void removeByDocumentId(Long documentId) {
        EmbeddingStore<TextSegment> store = delegate.get();
        if (store instanceof DocumentAwareEmbeddingStore) {
            ((DocumentAwareEmbeddingStore) store).removeByDocumentId(documentId);
        }
    }

    public EmbeddingStore<TextSegment> getDelegate() {
        return delegate.get();
    }
}
