package com.caregiver.carelink.store;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.caregiver.carelink.utils.RedisUtils;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * 基于 Redis 的对话记忆持久化存储
 * <p>
 * 将 LangChain4j 的对话消息序列化为 JSON 存储在 Redis 中，
 * 实现服务重启后对话记忆不丢失。
 * <p>
 * 支持完整的消息类型序列化，包括：
 * - UserMessage（用户消息）
 * - AiMessage（AI回复，含工具调用请求 toolExecutionRequests）
 * - SystemMessage（系统消息）
 * - ToolExecutionResultMessage（工具执行结果）
 *
 * @author CareLink
 * @since 2026-02-11
 */
public class RedisChatMemoryStore implements ChatMemoryStore {

    private static final Logger log = LoggerFactory.getLogger(RedisChatMemoryStore.class);

    /** Redis 键前缀 */
    private static final String KEY_PREFIX = "chat:memory:";

    private final RedisUtils redisUtils;
    private final long expireSeconds;

    public RedisChatMemoryStore(RedisUtils redisUtils, long expireSeconds) {
        this.redisUtils = redisUtils;
        this.expireSeconds = expireSeconds;
    }

    @Override
    public List<ChatMessage> getMessages(Object memoryId) {
        try {
            Object raw = redisUtils.get(KEY_PREFIX + memoryId);
            if (raw == null) {
                return new ArrayList<>();
            }
            String json = raw.toString();
            return deserializeMessages(json);
        } catch (Exception e) {
            log.error("从Redis读取对话记忆失败, memoryId={}", memoryId, e);
            return new ArrayList<>();
        }
    }

    @Override
    public void updateMessages(Object memoryId, List<ChatMessage> messages) {
        try {
            String json = serializeMessages(messages);
            redisUtils.set(KEY_PREFIX + memoryId, json, expireSeconds);
        } catch (Exception e) {
            log.error("对话记忆写入Redis失败, memoryId={}", memoryId, e);
        }
    }

    @Override
    public void deleteMessages(Object memoryId) {
        try {
            redisUtils.del(KEY_PREFIX + memoryId);
        } catch (Exception e) {
            log.error("对话记忆从Redis删除失败, memoryId={}", memoryId, e);
        }
    }

    // ========================= 序列化/反序列化 =========================

    /**
     * 将消息列表序列化为 JSON 字符串。
     * <p>
     * 完整保存所有消息类型，包括工具调用请求和工具执行结果，
     * 确保模型在多轮工具调用场景中能正确感知已完成的工具调用，避免重复调用。
     */
    private String serializeMessages(List<ChatMessage> messages) {
        JSONArray array = new JSONArray();
        for (ChatMessage msg : messages) {
            JSONObject obj = new JSONObject();
            if (msg instanceof UserMessage) {
                obj.put("type", "USER");
                obj.put("text", ((UserMessage) msg).singleText());
            } else if (msg instanceof AiMessage) {
                AiMessage aiMsg = (AiMessage) msg;
                obj.put("type", "AI");
                obj.put("text", aiMsg.text());
                // 保存工具调用请求（如果有）
                if (aiMsg.hasToolExecutionRequests()) {
                    JSONArray toolRequests = new JSONArray();
                    for (ToolExecutionRequest req : aiMsg.toolExecutionRequests()) {
                        JSONObject reqObj = new JSONObject();
                        reqObj.put("id", req.id());
                        reqObj.put("name", req.name());
                        reqObj.put("arguments", req.arguments());
                        toolRequests.add(reqObj);
                    }
                    obj.put("toolExecutionRequests", toolRequests);
                }
            } else if (msg instanceof ToolExecutionResultMessage) {
                ToolExecutionResultMessage toolResult = (ToolExecutionResultMessage) msg;
                obj.put("type", "TOOL_RESULT");
                obj.put("id", toolResult.id());
                obj.put("toolName", toolResult.toolName());
                obj.put("text", toolResult.text());
            } else if (msg instanceof SystemMessage) {
                obj.put("type", "SYSTEM");
                obj.put("text", ((SystemMessage) msg).text());
            } else {
                // 其他未知类型跳过
                log.debug("跳过未知消息类型的序列化: {}", msg.getClass().getSimpleName());
                continue;
            }
            array.add(obj);
        }
        return array.toJSONString();
    }

    /**
     * 从 JSON 字符串反序列化为消息列表。
     * <p>
     * 完整还原所有消息类型，包括 AI 的工具调用请求和工具执行结果。
     */
    private List<ChatMessage> deserializeMessages(String json) {
        List<ChatMessage> messages = new ArrayList<>();
        JSONArray array = JSON.parseArray(json);
        if (array == null) {
            return messages;
        }
        for (int i = 0; i < array.size(); i++) {
            JSONObject obj = array.getJSONObject(i);
            String type = obj.getString("type");
            if (type == null) continue;

            switch (type) {
                case "USER": {
                    String text = obj.getString("text");
                    if (text != null) {
                        messages.add(UserMessage.from(text));
                    }
                    break;
                }
                case "AI": {
                    String text = obj.getString("text");
                    JSONArray toolRequests = obj.getJSONArray("toolExecutionRequests");
                    if (toolRequests != null && !toolRequests.isEmpty()) {
                        // 还原带工具调用请求的 AiMessage
                        List<ToolExecutionRequest> requests = new ArrayList<>();
                        for (int j = 0; j < toolRequests.size(); j++) {
                            JSONObject reqObj = toolRequests.getJSONObject(j);
                            requests.add(ToolExecutionRequest.builder()
                                    .id(reqObj.getString("id"))
                                    .name(reqObj.getString("name"))
                                    .arguments(reqObj.getString("arguments"))
                                    .build());
                        }
                        messages.add(AiMessage.from(text, requests));
                    } else if (text != null) {
                        messages.add(AiMessage.from(text));
                    }
                    break;
                }
                case "TOOL_RESULT": {
                    String id = obj.getString("id");
                    String toolName = obj.getString("toolName");
                    String text = obj.getString("text");
                    if (text != null) {
                        messages.add(ToolExecutionResultMessage.from(id, toolName, text));
                    }
                    break;
                }
                case "SYSTEM": {
                    String text = obj.getString("text");
                    if (text != null) {
                        messages.add(SystemMessage.from(text));
                    }
                    break;
                }
                default:
                    log.debug("跳过未知消息类型的反序列化: {}", type);
                    break;
            }
        }
        return messages;
    }
}
