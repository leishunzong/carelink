package com.caregiver.carelink.prop;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * AI助手配置属性
 *
 * @author CareLink
 * @since 2026-02-11
 */
@Data
@Component
@ConfigurationProperties(prefix = "ai")
public class AiProperties {

    /** 模型配置 */
    private ModelConfig model = new ModelConfig();

    /** 聊天配置 */
    private ChatConfig chat = new ChatConfig();

    /** RAG 配置 */
    private RagConfig rag = new RagConfig();

    @Data
    public static class ModelConfig {
        /** API密钥 */
        private String apiKey;
        /** 模型API地址（支持OpenAI协议的任何服务商） */
        private String baseUrl;
        /** 模型名称 */
        private String modelName;
        /** 温度参数（0~1，越大越随机） */
        private Double temperature = 0.7;
        /** 最大输出token数 */
        private Integer maxTokens = 2048;
        /** 调用模型 API 的 HTTP 超时时间（秒），流式响应时单次请求可能较久，建议 120~300 */
        private Integer timeoutSeconds = 180;
    }

    @Data
    public static class ChatConfig {
        /** 对话记忆保留的最大消息条数 */
        private Integer memoryMaxMessages = 20;
        /** 系统提示词文件路径（classpath 下） */
        private String systemPromptPath = "prompts/care-assistant.txt";
        /** 对话记忆 Redis 过期时间（秒），默认 7 天 */
        private Long memoryExpireSeconds = 604800L;
    }

    @Data
    public static class RagConfig {
        /** 是否启用 RAG */
        private Boolean enabled = true;
        /** 知识库文件目录（classpath 下） */
        private String knowledgePath = "knowledge/";
        /** 检索返回的最大文档片段数 */
        private Integer maxResults = 3;
        /** 相似度阈值（0~1，低于此值的结果会被过滤） */
        private Double minScore = 0.6;
    }
}
