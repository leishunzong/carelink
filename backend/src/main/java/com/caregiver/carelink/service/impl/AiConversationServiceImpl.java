package com.caregiver.carelink.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.caregiver.carelink.entity.AiConversation;
import com.caregiver.carelink.entity.AiMessage;
import com.caregiver.carelink.mapper.AiConversationMapper;
import com.caregiver.carelink.mapper.AiMessageMapper;
import com.caregiver.carelink.service.AiConversationService;
import com.caregiver.carelink.vo.AiConversationVO;
import com.caregiver.carelink.vo.AiMessageVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.time.LocalDateTime;

@Service
public class AiConversationServiceImpl extends ServiceImpl<AiConversationMapper, AiConversation> implements AiConversationService {

    private static final Logger log = LoggerFactory.getLogger(AiConversationServiceImpl.class);

    @Resource
    private AiMessageMapper aiMessageMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void appendMessage(Long userId, String conversationId, String userMessage, String assistantReply) {
        if (userId == null || !StringUtils.hasText(conversationId)) {
            return;
        }
        LambdaQueryWrapper<AiConversation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AiConversation::getUserId, userId)
                .eq(AiConversation::getConversationId, conversationId);
        AiConversation conv = getOne(wrapper);
        if (conv == null) {
            conv = new AiConversation();
            conv.setUserId(userId);
            conv.setConversationId(conversationId);
            // 简单用首条问题截断生成标题
            String title = userMessage;
            if (title != null && title.length() > 30) {
                title = title.substring(0, 30);
            }
            conv.setTitle(title);
            conv.setMessageCount(0);
            conv.setStatus(1);
            conv.setCreateTime(LocalDateTime.now());
            conv.setUpdateTime(LocalDateTime.now());
            save(conv);
        }

        int startSeq = conv.getMessageCount() != null ? conv.getMessageCount() + 1 : 1;

        AiMessage userMsg = new AiMessage();
        userMsg.setConversationId(conv.getId());
        userMsg.setUserId(userId);
        userMsg.setRole(1);
        userMsg.setContent(userMessage);
        userMsg.setSeq(startSeq);
        userMsg.setCreateTime(LocalDateTime.now());
        aiMessageMapper.insert(userMsg);

        AiMessage aiMsg = new AiMessage();
        aiMsg.setConversationId(conv.getId());
        aiMsg.setUserId(userId);
        aiMsg.setRole(2);
        aiMsg.setContent(assistantReply);
        aiMsg.setSeq(startSeq + 1);
        aiMsg.setCreateTime(LocalDateTime.now());
        aiMessageMapper.insert(aiMsg);

        conv.setLastQuestion(userMessage);
        conv.setLastAnswer(assistantReply);
        conv.setMessageCount(startSeq + 1);
        conv.setUpdateTime(LocalDateTime.now());
        updateById(conv);
    }

    @Override
    public Page<AiConversationVO> pageUserConversations(Long userId, long current, long size) {
        Page<AiConversation> page = new Page<>(current, size);
        LambdaQueryWrapper<AiConversation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AiConversation::getUserId, userId)
                .eq(AiConversation::getStatus, 1)
                .orderByDesc(AiConversation::getIsPinned)
                .orderByDesc(AiConversation::getUpdateTime);
        page = this.page(page, wrapper);

        Page<AiConversationVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        voPage.setRecords(page.convert(entity -> {
            AiConversationVO vo = new AiConversationVO();
            vo.setId(entity.getId());
            vo.setConversationId(entity.getConversationId());
            vo.setTitle(entity.getTitle());
            vo.setLastQuestion(entity.getLastQuestion());
            vo.setLastAnswer(entity.getLastAnswer());
            vo.setMessageCount(entity.getMessageCount());
            vo.setIsPinned(entity.getIsPinned());
            vo.setIsFavorite(entity.getIsFavorite());
            vo.setUpdateTime(entity.getUpdateTime());
            return vo;
        }).getRecords());
        return voPage;
    }

    @Override
    public Page<AiMessageVO> pageConversationMessages(Long userId, String conversationId, long current, long size) {
        if (userId == null || !StringUtils.hasText(conversationId)) {
            return new Page<>();
        }
        LambdaQueryWrapper<AiConversation> convWrapper = new LambdaQueryWrapper<>();
        convWrapper.eq(AiConversation::getUserId, userId)
                .eq(AiConversation::getConversationId, conversationId)
                .eq(AiConversation::getStatus, 1);
        AiConversation conv = getOne(convWrapper);
        if (conv == null) {
            return new Page<>();
        }
        Page<AiMessage> page = new Page<>(current, size);
        LambdaQueryWrapper<AiMessage> msgWrapper = new LambdaQueryWrapper<>();
        msgWrapper.eq(AiMessage::getConversationId, conv.getId())
                .orderByAsc(AiMessage::getSeq);
        page = aiMessageMapper.selectPage(page, msgWrapper);

        Page<AiMessageVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        voPage.setRecords(page.convert(entity -> {
            AiMessageVO vo = new AiMessageVO();
            vo.setId(entity.getId());
            vo.setRole(entity.getRole());
            vo.setContent(entity.getContent());
            vo.setSeq(entity.getSeq());
            vo.setCreateTime(entity.getCreateTime());
            return vo;
        }).getRecords());
        return voPage;
    }

    @Override
    public void renameConversation(Long userId, String conversationId, String newTitle) {
        if (userId == null || !StringUtils.hasText(conversationId)) {
            return;
        }
        LambdaQueryWrapper<AiConversation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AiConversation::getUserId, userId)
                .eq(AiConversation::getConversationId, conversationId)
                .eq(AiConversation::getStatus, 1);
        AiConversation conv = getOne(wrapper);
        if (conv == null) {
            return;
        }
        if (newTitle != null && newTitle.length() > 100) {
            newTitle = newTitle.substring(0, 100);
        }
        conv.setTitle(newTitle);
        conv.setUpdateTime(LocalDateTime.now());
        updateById(conv);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteConversation(Long userId, String conversationId) {
        if (userId == null || !StringUtils.hasText(conversationId)) {
            return;
        }
        LambdaQueryWrapper<AiConversation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AiConversation::getUserId, userId)
                .eq(AiConversation::getConversationId, conversationId)
                .eq(AiConversation::getStatus, 1);
        AiConversation conv = getOne(wrapper);
        if (conv == null) {
            return;
        }
        Long convId = conv.getId();
        // 删除消息
        LambdaQueryWrapper<AiMessage> msgWrapper = new LambdaQueryWrapper<>();
        msgWrapper.eq(AiMessage::getConversationId, convId);
        aiMessageMapper.delete(msgWrapper);
        // 软删会话
        conv.setStatus(0);
        conv.setUpdateTime(LocalDateTime.now());
        updateById(conv);
        log.info("删除AI会话 userId={}, conversationId={}, 消息数={}", userId, conversationId, conv.getMessageCount());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void clearUserConversations(Long userId) {
        if (userId == null) {
            return;
        }
        LambdaQueryWrapper<AiConversation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AiConversation::getUserId, userId)
                .eq(AiConversation::getStatus, 1);
        java.util.List<AiConversation> list = list(wrapper);
        if (list.isEmpty()) {
            return;
        }
        java.util.List<Long> convIds = new java.util.ArrayList<>();
        for (AiConversation c : list) {
            convIds.add(c.getId());
        }
        // 删除所有消息
        LambdaQueryWrapper<AiMessage> msgWrapper = new LambdaQueryWrapper<>();
        msgWrapper.in(AiMessage::getConversationId, convIds);
        aiMessageMapper.delete(msgWrapper);
        // 软删会话
        for (AiConversation c : list) {
            c.setStatus(0);
            c.setUpdateTime(LocalDateTime.now());
        }
        updateBatchById(list);
        log.info("清空用户全部AI会话 userId={}, 会话数={}", userId, list.size());
    }

    @Override
    public void updatePinnedStatus(Long userId, String conversationId, boolean pinned) {
        if (userId == null || !StringUtils.hasText(conversationId)) {
            return;
        }
        LambdaQueryWrapper<AiConversation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AiConversation::getUserId, userId)
                .eq(AiConversation::getConversationId, conversationId)
                .eq(AiConversation::getStatus, 1);
        AiConversation conv = getOne(wrapper);
        if (conv == null) {
            return;
        }
        conv.setIsPinned(pinned ? 1 : 0);
        conv.setUpdateTime(LocalDateTime.now());
        updateById(conv);
    }

    @Override
    public void updateFavoriteStatus(Long userId, String conversationId, boolean favorite) {
        if (userId == null || !StringUtils.hasText(conversationId)) {
            return;
        }
        LambdaQueryWrapper<AiConversation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AiConversation::getUserId, userId)
                .eq(AiConversation::getConversationId, conversationId)
                .eq(AiConversation::getStatus, 1);
        AiConversation conv = getOne(wrapper);
        if (conv == null) {
            return;
        }
        conv.setIsFavorite(favorite ? 1 : 0);
        conv.setUpdateTime(LocalDateTime.now());
        updateById(conv);
    }
}

