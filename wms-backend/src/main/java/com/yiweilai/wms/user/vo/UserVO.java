package com.yiweilai.wms.user.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户展示对象
 */
@Data
public class UserVO {

    private Long id;
    private String username;
    private String realName;
    private String phone;
    private String email;
    private Integer status;
    private List<String> roles;
    private List<Long> roleIds;
    private LocalDateTime createdAt;
}
