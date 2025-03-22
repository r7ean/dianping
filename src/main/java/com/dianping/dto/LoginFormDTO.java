package com.dianping.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "用户登录请求实体")
public class LoginFormDTO {
    /**
     * 登录手机号
     */
    @Schema(description = "登录手机号", example = "12345678910")
    private String phone;

    /**
     * 用户登录验证码
     */
    @Schema(description = "用户登录验证码", example = "1234")
    private String code;
    
    private String password;
}
