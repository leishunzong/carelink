package com.caregiver.carelink.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.caregiver.carelink.vo.AiConversationVO;
import com.caregiver.carelink.vo.AiMessageVO;

public interface AiConversationService {

    /**
     * 追加一轮对话（用户问题 + AI 回复），自动创建会话并更新统计信息
     */
    void appendMessage(Long userId, String conversationId, String userMessage, String assistantReply);

    /**
     * 分页查询当前用户的会话列表（按更新时间倒序）
     */
    Page<AiConversationVO> pageUserConversations(Long userId, long current, long size);

    /**
     * 分页查询某个会话下的消息列表（按 seq 正序）
     */
    Page<AiMessageVO> pageConversationMessages(Long userId, String conversationId, long current, long size);

    /**
     * 重命名会话标题（仅允许会话所属用户操作）
     */
    void renameConversation(Long userId, String conversationId, String newTitle);

    /**
     * 删除单个会话（软删），并清理其消息
     */
    void deleteConversation(Long userId, String conversationId);

    /**
     * 清空当前用户的所有会话（软删），并清理消息
     */
    void clearUserConversations(Long userId);

    /**
     * 设置或取消置顶
     */
    void updatePinnedStatus(Long userId, String conversationId, boolean pinned);

    /**
     * 设置或取消收藏
     */
    void updateFavoriteStatus(Long userId, String conversationId, boolean favorite);
}

