package com.caregiver.carelink.config;

import com.caregiver.carelink.prop.AiProperties;
import com.caregiver.carelink.service.RagDocumentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * 应用启动后从 DB 将 RAG 知识库文档全量加载到向量库
 *
 * @author CareLink
 * @since 2026-02-24
 */
@Component
@Order(100)
public class RagInitRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(RagInitRunner.class);

    @Resource
    private RagDocumentService ragDocumentService;

    @Resource
    private AiProperties aiProperties;

    @Override
    public void run(ApplicationArguments args) {
        if (!Boolean.TRUE.equals(aiProperties.getRag().getEnabled())) {
            return;
        }
        try {
            ragDocumentService.loadAllIntoStore();
            log.info("RAG 知识库初始化完成（已从 DB 加载到向量库）");
        } catch (Exception e) {
            log.error("RAG 知识库初始化失败", e);
        }
    }
}
