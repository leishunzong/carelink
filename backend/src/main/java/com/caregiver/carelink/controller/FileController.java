package com.caregiver.carelink.controller;

import com.caregiver.carelink.common.context.UserContextHolder;
import com.caregiver.carelink.common.result.Result;
import com.caregiver.carelink.utils.CosUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;

/**
 * 文件上传控制器
 *
 * @author CareLink
 * @since 2026-01-29
 */
@Api(tags = "文件管理")
@RestController
@RequestMapping("/file")
public class FileController {

    @Resource
    private CosUtils cosUtils;

    @ApiOperation("上传图片（需要登录）")
    @PostMapping("/upload/image")
    public Result<Map<String, String>> uploadImage(
            @ApiParam(value = "图片文件", required = true) @RequestParam("file") MultipartFile file) {

        // 验证文件类型
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            return Result.fail("只能上传图片文件");
        }

        // 验证文件大小（5MB）
        if (file.getSize() > 5 * 1024 * 1024) {
            return Result.fail("图片文件大小不能超过5MB");
        }

        // 上传到COS
        String url = cosUtils.uploadFile(file, "images/");

        Map<String, String> data = new HashMap<>();
        data.put("url", url);

        return Result.success("上传成功", data);
    }
}
