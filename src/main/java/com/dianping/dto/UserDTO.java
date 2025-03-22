package com.dianping.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "登录用户")
public class UserDTO {
    private Long id;
    private String nickName;
    private String icon;
}
