package com.caregiver.carelink.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.caregiver.carelink.common.exception.BusinessException;
import com.caregiver.carelink.entity.RagDocument;
import com.caregiver.carelink.mapper.RagDocumentMapper;
import com.caregiver.carelink.prop.AiProperties;
import com.caregiver.carelink.service.RagDocumentService;
import com.caregiver.carelink.store.DocumentAwareEmbeddingStore;
import com.caregiver.carelink.store.InMemoryDocumentAwareEmbeddingStore;
import com.caregiver.carelink.store.RebuildableEmbeddingStore;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

/**
 * RAG 知识库文档服务实现
 *
 * @author CareLink
 * @since 2026-02-24
 */
@Service
public class RagDocumentServiceImpl extends ServiceImpl<RagDocumentMapper, RagDocument> implements RagDocumentService {

    private static final Logger log = LoggerFactory.getLogger(RagDocumentServiceImpl.class);
    private static final int SPLIT_MAX_SEGMENT_SIZE = 500;
    private static final int SPLIT_OVERLAP = 50;

    @Resource
    private RebuildableEmbeddingStore embeddingStore;

    @Resource
    private EmbeddingModel embeddingModel;

    @Resource
    private AiProperties aiProperties;

    @Override
    public RagDocument addDocument(String title, String fileName, String content) {
        if (!StringUtils.hasText(content)) {
            throw new BusinessException("文档内容不能为空");
        }
        RagDocument doc = new RagDocument();
        doc.setTitle(StringUtils.hasText(title) ? title : (fileName != null ? fileName : "未命名"));
        doc.setFileName(fileName);
        doc.setContent(content);
        save(doc);

        List<TextSegment> segments = splitContent(content);
        if (segments.isEmpty()) {
            log.warn("文档 {} 切片后为空，已落库但未加入向量库", doc.getId());
            return doc;
        }
        // 为每个 segment 打上文档 ID，便于删除时只移除本文档向量
        List<TextSegment> segmentsWithMeta = addDocumentIdMetadata(segments, doc.getId());
        List<Embedding> embeddings = embeddingModel.embedAll(segmentsWithMeta).content();
        embeddingStore.addAll(embeddings, segmentsWithMeta);
        log.info("RAG 文档已加入向量库: id={}, title={}, 片段数={}", doc.getId(), doc.getTitle(), segments.size());
        return doc;
    }

    @Override
    public List<RagDocument> listDocuments() {
        return list(new LambdaQueryWrapper<RagDocument>().orderByDesc(RagDocument::getCreateTime));
    }

    @Override
    public void deleteDocumentAndRebuild(Long id) {
        if (!removeById(id)) {
            throw new BusinessException("文档不存在或已删除");
        }
        // 仅移除该文档的向量，不重建整个库，避免影响体验
        embeddingStore.removeByDocumentId(id);
        log.info("RAG 文档已删除并移除其向量: id={}", id);
    }

    @Override
    public void loadAllIntoStore() {
        if (!Boolean.TRUE.equals(aiProperties.getRag().getEnabled())) {
            return;
        }
        List<RagDocument> docs = list();
        DocumentAwareEmbeddingStore newStore = new InMemoryDocumentAwareEmbeddingStore();
        DocumentSplitter splitter = DocumentSplitters.recursive(SPLIT_MAX_SEGMENT_SIZE, SPLIT_OVERLAP);

        for (RagDocument doc : docs) {
            if (!StringUtils.hasText(doc.getContent())) continue;
            Document document = Document.from(doc.getContent());
            List<TextSegment> segments = splitter.split(document);
            if (segments.isEmpty()) continue;
            List<TextSegment> segmentsWithMeta = addDocumentIdMetadata(segments, doc.getId());
            List<Embedding> embeddings = embeddingModel.embedAll(segmentsWithMeta).content();
            newStore.addAll(embeddings, segmentsWithMeta);
        }
        embeddingStore.replaceDelegate(newStore);
        log.info("RAG 向量库已重建: 文档数={}", docs.size());
    }

    /** 为 segments 增加 ragDocumentId 元数据，便于按文档删除向量 */
    private List<TextSegment> addDocumentIdMetadata(List<TextSegment> segments, Long documentId) {
        if (documentId == null) return segments;
        String key = InMemoryDocumentAwareEmbeddingStore.getRagDocumentIdKey();
        List<TextSegment> out = new ArrayList<>(segments.size());
        for (TextSegment seg : segments) {
            Metadata meta = new Metadata();
            meta.put(key, documentId);
            out.add(TextSegment.from(seg.text(), meta));
        }
        return out;
    }

    private List<TextSegment> splitContent(String content) {
        DocumentSplitter splitter = DocumentSplitters.recursive(SPLIT_MAX_SEGMENT_SIZE, SPLIT_OVERLAP);
        return new ArrayList<>(splitter.split(Document.from(content)));
    }
}
