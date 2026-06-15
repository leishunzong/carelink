package com.caregiver.carelink.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.caregiver.carelink.common.context.UserContextHolder;
import com.caregiver.carelink.common.result.PageResult;
import com.caregiver.carelink.common.result.Result;
import com.caregiver.carelink.dto.ChatRequestDTO;
import com.caregiver.carelink.entity.ServiceSubject;
import com.caregiver.carelink.service.AiConversationService;
import com.caregiver.carelink.service.CareAssistant;
import com.caregiver.carelink.service.ServiceSubjectService;
import com.caregiver.carelink.tool.CareBusinessTools;
import com.caregiver.carelink.vo.AiConversationVO;
import com.caregiver.carelink.vo.AiMessageVO;
import com.caregiver.carelink.vo.ChatResponseVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import javax.annotation.Resource;
import java.io.IOException;
import java.time.LocalDate;
import java.time.Period;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * AI智能助手控制器
 *
 * @author CareLink
 * @since 2026-02-11
 */
@Api(tags = "AI智能助手")
@RestController
@RequestMapping("/ai")
public class AiController {

    private static final Logger log = LoggerFactory.getLogger(AiController.class);

    @Resource
    private CareAssistant careAssistant;

    @Resource
    private AiConversationService aiConversationService;

    @Resource
    private ServiceSubjectService serviceSubjectService;

    /**
     * 同步聊天（等待完整回复后返回）
     * 内部通过收集 TokenStream 全部 token 实现
     */
    @ApiOperation("AI对话（同步）")
    @PostMapping("/chat")
    public Result<ChatResponseVO> chat(@Validated @RequestBody ChatRequestDTO dto) {
        Long userId = UserContextHolder.getUserId();
        String conversationId = resolveConversationId(dto.getConversationId());
        String userMessage = buildUserMessage(dto);

        log.info("用户{}发起AI对话, conversationId={}, scene={}", userId, conversationId, dto.getScene());

        // 绑定 conversationId -> userId，供异步线程中的工具方法获取用户身份
        CareBusinessTools.bindUser(conversationId, userId);
        // 绑定前端传来的实时位置信息，供搜索附近护工等工具使用
        CareBusinessTools.bindLocation(conversationId, dto.getLongitude(), dto.getLatitude(), dto.getCityCode());
        try {
            CompletableFuture<String> future = new CompletableFuture<>();
            StringBuilder sb = new StringBuilder();

            careAssistant.chat(conversationId, userMessage)
                    .onPartialResponse(sb::append)
                    .onCompleteResponse(response -> future.complete(sb.toString()))
                    .onError(future::completeExceptionally)
                    .start();

            String reply = future.get(60, TimeUnit.SECONDS);

            // 持久化本轮对话
            aiConversationService.appendMessage(userId, conversationId, dto.getMessage(), reply);

            ChatResponseVO vo = ChatResponseVO.builder()
                    .reply(reply)
                    .conversationId(conversationId)
                    .build();
            return Result.success(vo);
        } catch (Exception e) {
            log.error("AI对话异常, conversationId={}", conversationId, e);
            return Result.fail("AI助手暂时无法回复，请稍后再试");
        } finally {
            CareBusinessTools.unbindUser(conversationId);
        }
    }

    /**
     * 流式聊天（SSE 逐 token 推送，前端体验更好）
     */
    @ApiOperation("AI对话（流式SSE）")
    @PostMapping("/chat/stream")
    public SseEmitter chatStream(@Validated @RequestBody ChatRequestDTO dto) {
        Long userId = UserContextHolder.getUserId();
        String conversationId = resolveConversationId(dto.getConversationId());
        String userMessage = buildUserMessage(dto);

        log.info("用户{}发起流式AI对话, conversationId={}, scene={}", userId, conversationId, dto.getScene());

        // 绑定 conversationId -> userId，供异步线程中的工具方法获取用户身份
        CareBusinessTools.bindUser(conversationId, userId);
        // 绑定前端传来的实时位置信息，供搜索附近护工等工具使用
        CareBusinessTools.bindLocation(conversationId, dto.getLongitude(), dto.getLatitude(), dto.getCityCode());

        // 5 分钟超时，留足时间给模型 API（含工具调用多轮）
        SseEmitter emitter = new SseEmitter(300_000L);
        AtomicBoolean emitterDone = new AtomicBoolean(false);

        StringBuilder sb = new StringBuilder();

        careAssistant.chat(conversationId, userMessage)
                .onPartialResponse(token -> {
                    sb.append(token);
                    if (emitterDone.get()) return;
                    try {
                        emitter.send(SseEmitter.event().name("token").data(token));
                    } catch (IOException e) {
                        log.warn("SSE发送失败: {}", e.getMessage());
                        emitterDone.set(true);
                        try { emitter.completeWithError(e); } catch (Exception ignored) {}
                    } catch (IllegalStateException e) {
                        emitterDone.set(true);
                    }
                })
                .onCompleteResponse(response -> {
                    CareBusinessTools.unbindUser(conversationId);
                    if (emitterDone.get()) return;
                    try {
                        aiConversationService.appendMessage(userId, conversationId, dto.getMessage(), sb.toString());
                        emitter.send(SseEmitter.event().name("done").data(conversationId));
                        emitter.complete();
                        emitterDone.set(true);
                    } catch (IOException e) {
                        log.warn("SSE完成事件发送失败: {}", e.getMessage());
                    } catch (IllegalStateException e) {
                        emitterDone.set(true);
                    }
                })
                .onError(error -> {
                    CareBusinessTools.unbindUser(conversationId);
                    log.error("AI流式对话异常, conversationId={}", conversationId, error);
                    if (emitterDone.get()) return;
                    try {
                        String msg = error != null ? error.getMessage() : "";
                        if (msg != null && msg.contains("exceeded") && msg.contains("sequential tool")) {
                            emitter.send(SseEmitter.event().name("error").data("对话轮次较多，请直接根据已返回的内容回复用户或稍后重试。"));
                        } else if (msg != null && (msg.contains("timed out") || msg.contains("TimeoutException"))) {
                            emitter.send(SseEmitter.event().name("error").data("请求超时，请稍后重试。"));
                        } else {
                            emitter.send(SseEmitter.event().name("error").data("AI助手暂时无法回复，请稍后再试。"));
                        }
                        emitter.complete();
                        emitterDone.set(true);
                    } catch (IOException e) {
                        log.warn("SSE 发送错误事件失败: {}", e.getMessage());
                    } catch (IllegalStateException e) {
                        // SSE 已因超时等被关闭，不再向 emitter 写
                    } finally {
                        emitterDone.set(true);
                    }
                })
                .start();

        emitter.onTimeout(() -> {
            log.warn("SSE超时, conversationId={}", conversationId);
            CareBusinessTools.unbindUser(conversationId);
            emitterDone.set(true);
        });
        emitter.onError(e -> {
            log.warn("SSE异常, conversationId={}", conversationId, e);
            CareBusinessTools.unbindUser(conversationId);
            emitterDone.set(true);
        });

        return emitter;
    }

    /**
     * 生成会话ID：客户端传了就用客户端的，没传则生成新的随机ID
     */
    private String resolveConversationId(String conversationId) {
        if (conversationId != null && !conversationId.trim().isEmpty()) {
            return conversationId;
        }
        return java.util.UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * 分页查询我的会话列表
     */
    @ApiOperation("查询我的AI会话列表")
    @GetMapping("/conversations")
    public Result<PageResult<AiConversationVO>> getMyConversations(
            @RequestParam(defaultValue = "1") Long current,
            @RequestParam(defaultValue = "10") Long size) {
        Long userId = UserContextHolder.getUserId();
        Page<AiConversationVO> page = aiConversationService.pageUserConversations(userId, current, size);
        return Result.success(PageResult.of(page));
    }

    /**
     * 查询某个会话的消息列表
     */
    @ApiOperation("查询指定会话的消息列表")
    @GetMapping("/conversation/{conversationId}/messages")
    public Result<PageResult<AiMessageVO>> getConversationMessages(
            @PathVariable String conversationId,
            @RequestParam(defaultValue = "1") Long current,
            @RequestParam(defaultValue = "50") Long size) {
        Long userId = UserContextHolder.getUserId();
        Page<AiMessageVO> page = aiConversationService.pageConversationMessages(userId, conversationId, current, size);
        return Result.success(PageResult.of(page));
    }

    /**
     * 重命名会话标题
     */
    @ApiOperation("重命名AI会话标题")
    @PutMapping("/conversation/{conversationId}/title")
    public Result<Void> renameConversation(
            @PathVariable String conversationId,
            @RequestParam String title) {
        Long userId = UserContextHolder.getUserId();
        aiConversationService.renameConversation(userId, conversationId, title);
        return Result.success("修改成功");
    }

    /**
     * 删除单个会话
     */
    @ApiOperation("删除单个AI会话")
    @DeleteMapping("/conversation/{conversationId}")
    public Result<Void> deleteConversation(@PathVariable String conversationId) {
        Long userId = UserContextHolder.getUserId();
        aiConversationService.deleteConversation(userId, conversationId);
        return Result.success("删除成功");
    }

    /**
     * 清空当前用户的所有会话
     */
    @ApiOperation("清空我的所有AI会话")
    @DeleteMapping("/conversations/clear")
    public Result<Void> clearConversations() {
        Long userId = UserContextHolder.getUserId();
        aiConversationService.clearUserConversations(userId);
        return Result.success("已清空");
    }

    /**
     * 设置或取消会话置顶
     */
    @ApiOperation("设置/取消会话置顶")
    @PutMapping("/conversation/{conversationId}/pin")
    public Result<Void> pinConversation(
            @PathVariable String conversationId,
            @RequestParam boolean pinned) {
        Long userId = UserContextHolder.getUserId();
        aiConversationService.updatePinnedStatus(userId, conversationId, pinned);
        return Result.success("操作成功");
    }

    /**
     * 设置或取消会话收藏
     */
    @ApiOperation("设置/取消会话收藏")
    @PutMapping("/conversation/{conversationId}/favorite")
    public Result<Void> favoriteConversation(
            @PathVariable String conversationId,
            @RequestParam boolean favorite) {
        Long userId = UserContextHolder.getUserId();
        aiConversationService.updateFavoriteStatus(userId, conversationId, favorite);
        return Result.success("操作成功");
    }

    // ==================== 场景消息构建 ====================

    /**
     * 根据场景构建用户消息。
     * 当 scene = "care_recommend" 时，在用户消息前注入引导指令，
     * 让 AI 主动开始护理方案推荐对话流程。
     */
    private String buildUserMessage(ChatRequestDTO dto) {
        String scene = dto.getScene();
        String message = dto.getMessage();

        if ("care_recommend".equals(scene)) {
            Long userId = UserContextHolder.getUserId();
            // 先查询用户已维护的服务对象
            List<ServiceSubject> subjects = null;
            try {
                subjects = serviceSubjectService.getSubjectList(userId);
            } catch (Exception e) {
                log.warn("查询用户服务对象失败, userId={}", userId, e);
            }

            StringBuilder sb = new StringBuilder();
            sb.append("[系统指令：用户进入了「智能护理方案推荐」场景。");

            if (subjects != null && !subjects.isEmpty()) {
                // 有服务对象：展示列表让用户选择
                sb.append("该用户已维护了以下服务对象信息：\n");
                for (ServiceSubject s : subjects) {
                    sb.append("- ").append(s.getName());
                    if (s.getRelationship() != null) {
                        sb.append("（").append(s.getRelationship()).append("）");
                    }
                    if (s.getBirthday() != null) {
                        int age = Period.between(s.getBirthday(), LocalDate.now()).getYears();
                        sb.append(" ").append(age).append("岁");
                    }
                    if (s.getGender() != null) {
                        sb.append(" ").append(s.getGender() == 1 ? "男" : "女");
                    }
                    if (s.getSelfCareAbility() != null && !s.getSelfCareAbility().isEmpty()) {
                        sb.append(" 自理能力:").append(s.getSelfCareAbility());
                    }
                    if (s.getMedicalHistory() != null && !s.getMedicalHistory().isEmpty()) {
                        sb.append(" 病史:").append(s.getMedicalHistory());
                    }
                    if (s.getIsDefault() != null && s.getIsDefault() == 1) {
                        sb.append(" [默认]");
                    }
                    sb.append("\n");
                }
                sb.append("\n请你友好地打招呼，然后将以上服务对象信息以清晰的列表形式展示给用户，")
                  .append("让用户选择要为哪位服务对象推荐护理方案。")
                  .append("如果只有一位服务对象，也要展示其信息并确认是否为其推荐。")
                  .append("用户选择后，根据该服务对象的已有信息（年龄、病史、自理能力等），")
                  .append("判断信息是否足够进行推荐：")
                  .append("如果信息已经比较完整，可以直接询问用户的护理需求偏好后调用 recommendCarePlan 工具；")
                  .append("如果关键信息缺失（如病史、自理能力为空），再针对性地补充询问。")
                  .append("推荐完成后主动引导用户进入下单流程。]");
            } else {
                // 无服务对象：文字引导用户描述
                sb.append("该用户暂未维护服务对象信息。")
                  .append("请你作为专业护理顾问，主动向用户打招呼，并引导用户逐步描述以下信息：")
                  .append("1. 服务对象的基本情况（年龄、性别）；")
                  .append("2. 健康状况和病史（如高血压、糖尿病、阿尔茨海默等）；")
                  .append("3. 自理能力（能否自行吃饭、走路、上厕所等）；")
                  .append("4. 主要的护理需求（日常照料、康复训练、专业护理等）。")
                  .append("注意：不要一次性问完所有问题，先友好地打招呼并询问第一个问题。")
                  .append("收集到足够信息后，调用 recommendCarePlan 工具生成推荐方案，")
                  .append("然后主动引导用户进入下单流程。]");
            }

            if (message != null && !message.trim().isEmpty()) {
                sb.append("\n用户补充说明：").append(message);
            }
            return sb.toString();
        }

        return message;
    }
}
