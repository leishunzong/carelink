package com.caregiver.carelink.service;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.service.UserMessage;

/**
 * 护联AI智能助手（LangChain4j AI Service 接口）
 * <p>
 * LangChain4j 会自动生成代理实现，统一处理：
 * - 系统提示词注入
 * - 对话记忆管理（按 memoryId 隔离不同用户/会话）
 * - 流式模型调用
 * <p>
 * 返回 TokenStream 以支持 SSE 流式输出；同步场景通过收集完整流实现。
 *
 * @author CareLink
 * @since 2026-02-11
 */
public interface CareAssistant {

    /**
     * AI 聊天（流式）
     *
     * @param memoryId 会话ID，用于隔离不同用户/会话的对话记忆
     * @param message  用户消息内容
     * @return TokenStream 流式响应，可逐 token 消费
     */
    TokenStream chat(@MemoryId String memoryId, @UserMessage String message);
}
