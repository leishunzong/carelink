package com.caregiver.carelink.service.impl;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AiSummaryServiceImplTest {

    @Test
    void generateCareRecommendationReturnsFriendlyMessageWhenAiModelIsDisabled() {
        AiSummaryServiceImpl service = new AiSummaryServiceImpl();

        String result = service.generateCareRecommendation("老人行动不便，需要日常照护");

        assertThat(result).contains("AI助手未启用");
    }
}
