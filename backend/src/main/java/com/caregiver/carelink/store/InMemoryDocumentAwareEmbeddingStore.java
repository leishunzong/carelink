package com.caregiver.carelink.store;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.EmbeddingMatch;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 内存实现：支持按文档 ID 删除的向量库，删除时只移除该文档的向量，无需全量重建。
 *
 * @author CareLink
 * @since 2026-02-26
 */
public class InMemoryDocumentAwareEmbeddingStore implements DocumentAwareEmbeddingStore {

    private static final String RAG_DOCUMENT_ID = "ragDocumentId";

    private final List<Entry> entries = new CopyOnWriteArrayList<>();

    private static final class Entry {
        final String id;
        final Embedding embedding;
        final TextSegment segment;

        Entry(String id, Embedding embedding, TextSegment segment) {
            this.id = id;
            this.embedding = embedding;
            this.segment = segment;
        }
    }

    @Override
    public String add(Embedding embedding) {
        return add(embedding, TextSegment.from(""));
    }

    @Override
    public void add(String id, Embedding embedding) {
        entries.add(new Entry(id, embedding, TextSegment.from("")));
    }

    @Override
    public String add(Embedding embedding, TextSegment segment) {
        String id = UUID.randomUUID().toString();
        entries.add(new Entry(id, embedding, segment));
        return id;
    }

    @Override
    public List<String> addAll(List<Embedding> embeddings) {
        List<String> ids = new ArrayList<>(embeddings.size());
        for (Embedding e : embeddings) {
            ids.add(add(e));
        }
        return ids;
    }

    @Override
    public List<String> addAll(List<Embedding> embeddings, List<TextSegment> segments) {
        if (embeddings.size() != segments.size()) {
            throw new IllegalArgumentException("embeddings and segments size must match");
        }
        List<String> ids = new ArrayList<>(embeddings.size());
        for (int i = 0; i < embeddings.size(); i++) {
            ids.add(add(embeddings.get(i), segments.get(i)));
        }
        return ids;
    }

    @Override
    public void addAll(List<String> ids, List<Embedding> embeddings, List<TextSegment> segments) {
        if (ids.size() != embeddings.size() || embeddings.size() != segments.size()) {
            throw new IllegalArgumentException("ids, embeddings and segments size must match");
        }
        for (int i = 0; i < embeddings.size(); i++) {
            entries.add(new Entry(ids.get(i), embeddings.get(i), segments.get(i)));
        }
    }

    @Override
    public EmbeddingSearchResult<TextSegment> search(EmbeddingSearchRequest request) {
        Embedding queryEmbedding = request.queryEmbedding();
        float[] queryVector = queryEmbedding.vector();
        int maxResults = request.maxResults();
        if (maxResults <= 0) maxResults = 5;
        Double minScore = request.minScore();

        List<EmbeddingMatch<TextSegment>> matches = new ArrayList<>();
        for (Entry e : entries) {
            double score = cosineSimilarity(queryVector, e.embedding.vector());
            if (minScore != null && score < minScore) continue;
            matches.add(new EmbeddingMatch<>(score, e.id, e.embedding, e.segment));
        }
        matches.sort((a, b) -> Double.compare(b.score(), a.score()));
        int limit = Math.min(maxResults, matches.size());
        List<EmbeddingMatch<TextSegment>> top = matches.subList(0, limit);

        return new EmbeddingSearchResult<>(top);
    }

    @Override
    public void removeByDocumentId(Long documentId) {
        if (documentId == null) return;
        entries.removeIf(e -> {
            Long docId = getRagDocumentId(e.segment);
            return docId != null && docId.equals(documentId);
        });
    }

    private static Long getRagDocumentId(TextSegment segment) {
        if (segment == null || segment.metadata() == null) return null;
        try {
            Long v = segment.metadata().getLong(RAG_DOCUMENT_ID);
            return v;
        } catch (Exception e) {
            String s = segment.metadata().getString(RAG_DOCUMENT_ID);
            if (s == null) return null;
            try {
                return Long.parseLong(s);
            } catch (NumberFormatException ex) {
                return null;
            }
        }
    }

    /**
     * 供 RAG 服务在写入向量时给 segment 打上文档 ID 的 key
     */
    public static String getRagDocumentIdKey() {
        return RAG_DOCUMENT_ID;
    }

    private static double cosineSimilarity(float[] a, float[] b) {
        if (a == null || b == null || a.length != b.length) return 0;
        double dot = 0, normA = 0, normB = 0;
        for (int i = 0; i < a.length; i++) {
            dot += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }
        double denom = Math.sqrt(normA) * Math.sqrt(normB);
        return denom <= 0 ? 0 : (dot / denom);
    }
}
