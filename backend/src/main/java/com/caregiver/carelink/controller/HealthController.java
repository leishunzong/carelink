package com.caregiver.carelink.controller;

import com.caregiver.carelink.common.result.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 健康检查控制器
 *
 * @author CareLink
 * @since 2026-01-29
 */
@Api(tags = "健康检查")
@RestController
@RequestMapping("/health")
public class HealthController {

    @ApiOperation("健康检查")
    @GetMapping
    public Result<Map<String, Object>> health() {
        Map<String, Object> data = new HashMap<>();
        data.put("status", "UP");
        data.put("timestamp", LocalDateTime.now());
        data.put("message", "护联系统运行正常");
        return Result.success(data);
    }
}
