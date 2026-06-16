package com.caregiver.carelink.utils;

import com.caregiver.carelink.common.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CosUtilsTest {

    @Test
    void uploadFileFailsClearlyWhenCosIsNotConfigured() {
        CosUtils cosUtils = new CosUtils();
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "avatar.png",
                "image/png",
                "image".getBytes());

        assertThatThrownBy(() -> cosUtils.uploadFile(file, "images/"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("文件存储服务未启用");
    }
}
