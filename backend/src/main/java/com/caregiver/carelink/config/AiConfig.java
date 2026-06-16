package com.caregiver.carelink.config;

import com.caregiver.carelink.prop.AiProperties;
import com.caregiver.carelink.service.CareAssistant;
import com.caregiver.carelink.store.InMemoryDocumentAwareEmbeddingStore;
import com.caregiver.carelink.store.RebuildableEmbeddingStore;
import com.caregiver.carelink.store.RedisChatMemoryStore;
import com.caregiver.carelink.tool.CareBusinessTools;
import com.caregiver.carelink.utils.RedisUtils;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.embedding.onnx.allminilml6v2.AllMiniLmL6V2EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * AI助手配置类
 * <p>
 * 统一装配：模型、对话记忆（Redis持久化）、业务工具（函数调用）、RAG（知识库检索）
 *
 * @author CareLink
 * @since 2026-02-11
 */
@Configuration
public class AiConfig {

    private static final Logger log = LoggerFactory.getLogger(AiConfig.class);

    @javax.annotation.Resource
    private AiProperties aiProperties;

    @javax.annotation.Resource
    private RedisUtils redisUtils;

    // ========================= 模型 =========================

    @Bean
    @ConditionalOnProperty(prefix = "ai.model", name = "enabled", havingValue = "true")
    public StreamingChatModel streamingChatModel() {
        AiProperties.ModelConfig model = aiProperties.getModel();
        log.info("初始化AI流式聊天模型: baseUrl={}, model={}", model.getBaseUrl(), model.getModelName());
        int timeoutSec = model.getTimeoutSeconds() != null && model.getTimeoutSeconds() > 0
                ? model.getTimeoutSeconds() : 180;
        log.info("模型 HTTP 超时: {} 秒", timeoutSec);
        return OpenAiStreamingChatModel.builder()
                .baseUrl(model.getBaseUrl())
                .apiKey(model.getApiKey())
                .modelName(model.getModelName())
                .temperature(model.getTemperature())
                .maxTokens(model.getMaxTokens())
                .timeout(Duration.ofSeconds(timeoutSec))
                .build();
    }

    /**
     * 同步聊天模型（用于工具内部调用，如 AI 评价摘要、智能推荐等）
     */
    @Bean
    @ConditionalOnProperty(prefix = "ai.model", name = "enabled", havingValue = "true")
    public ChatModel chatModel() {
        AiProperties.ModelConfig model = aiProperties.getModel();
        log.info("初始化AI同步聊天模型（用于内部工具调用）: baseUrl={}, model={}", model.getBaseUrl(), model.getModelName());
        return OpenAiChatModel.builder()
                .baseUrl(model.getBaseUrl())
                .apiKey(model.getApiKey())
                .modelName(model.getModelName())
                .temperature(0.3)
                .maxTokens(1024)
                .timeout(Duration.ofSeconds(60))
                .build();
    }

    // ========================= 对话记忆（Redis 持久化） =========================

    @Bean
    public ChatMemoryStore chatMemoryStore() {
        long expireSeconds = aiProperties.getChat().getMemoryExpireSeconds();
        log.info("初始化Redis对话记忆存储, 过期时间={}秒", expireSeconds);
        return new RedisChatMemoryStore(redisUtils, expireSeconds);
    }

    @Bean
    public ChatMemoryProvider chatMemoryProvider(ChatMemoryStore chatMemoryStore) {
        int maxMessages = aiProperties.getChat().getMemoryMaxMessages();
        return memoryId -> MessageWindowChatMemory.builder()
                .id(memoryId)
                .maxMessages(maxMessages)
                .chatMemoryStore(chatMemoryStore)
                .build();
    }

    // ========================= RAG（知识库检索增强） =========================

    @Bean
    public EmbeddingModel embeddingModel() {
        log.info("初始化本地嵌入模型 AllMiniLmL6V2（ONNX）");
        return new AllMiniLmL6V2EmbeddingModel();
    }

    /**
     * 向量库：使用支持按文档删除的实现，初始为空；启动后由 RagInitRunner 从 DB 全量加载。
     * 管理员上传文档会增量加入，删除文档时仅移除该文档向量，不重建整个库。
     */
    @Bean
    public RebuildableEmbeddingStore embeddingStore() {
        RebuildableEmbeddingStore wrapper = new RebuildableEmbeddingStore(new InMemoryDocumentAwareEmbeddingStore());
        if (!aiProperties.getRag().getEnabled()) {
            log.info("RAG 未启用，向量库保持空");
        }
        return wrapper;
    }

    @Bean
    public ContentRetriever contentRetriever(RebuildableEmbeddingStore embeddingStore,
                                              EmbeddingModel embeddingModel) {
        AiProperties.RagConfig ragConfig = aiProperties.getRag();
        return EmbeddingStoreContentRetriever.builder()
                .embeddingStore(embeddingStore)
                .embeddingModel(embeddingModel)
                .maxResults(ragConfig.getMaxResults())
                .minScore(ragConfig.getMinScore())
                .build();
    }

    // ========================= AI 助手（统一装配） =========================

    @Bean
    @ConditionalOnBean(StreamingChatModel.class)
    public CareAssistant careAssistant(StreamingChatModel streamingChatModel,
                                       ChatMemoryProvider chatMemoryProvider,
                                       ContentRetriever contentRetriever,
                                       CareBusinessTools careBusinessTools) {
        String systemPrompt = loadSystemPrompt(aiProperties.getChat().getSystemPromptPath());
        log.info("系统提示词已加载，长度={}字符", systemPrompt.length());

        // AiServices.builder(...) 返回的就是泛型 AiServices<CareAssistant>，直接作为 Builder 使用
        AiServices<CareAssistant> builder = AiServices.builder(CareAssistant.class)
                .streamingChatModel(streamingChatModel)
                .chatMemoryProvider(chatMemoryProvider)
                .systemMessageProvider(memoryId -> systemPrompt)
                // 函数调用：注册业务工具
                .tools(careBusinessTools)
                // 限制单轮对话内工具连续执行次数，超限后由 AiController 以 SSE error 事件友好结束，避免死循环
                // 推荐→下单联动场景可能需要多次工具调用（如 recommendCarePlan + getOrderPrerequisites），设为3次
                .maxSequentialToolsInvocations(3);

        // RAG：知识库检索增强
        if (aiProperties.getRag().getEnabled()) {
            builder.contentRetriever(contentRetriever);
            log.info("RAG 知识库检索已启用");
        }

        return builder.build();
    }

    // ========================= 辅助方法 =========================

    private String loadSystemPrompt(String path) {
        try (InputStream is = new ClassPathResource(path).getInputStream()) {
            return StreamUtils.copyToString(is, StandardCharsets.UTF_8).trim();
        } catch (IOException e) {
            log.error("加载系统提示词文件失败: {}", path, e);
            return "你是护联平台的AI智能助手，请用专业、耐心的语气回答用户关于居家养老护理的问题。";
        }
    }

}
