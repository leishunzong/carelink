package com.caregiver.carelink.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 登录响应VO
 *
 * @author CareLink
 * @since 2026-01-29
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ApiModel(description = "登录响应")
public class LoginVO {

    @ApiModelProperty("访问令牌")
    private String token;

    @ApiModelProperty("用户类型: user-用户, caregiver-护工")
    private String userType;
}
