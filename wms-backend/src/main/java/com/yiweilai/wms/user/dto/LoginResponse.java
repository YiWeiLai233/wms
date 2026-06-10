package com.yiweilai.wms.user.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 登录响应
 */
@Data
@Builder
public class LoginResponse {

    private Long userId;
    private String username;
    private String realName;
    private String token;
    private List<String> roles;
}
