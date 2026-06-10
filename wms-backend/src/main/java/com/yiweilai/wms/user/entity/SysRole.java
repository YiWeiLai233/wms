package com.yiweilai.wms.user.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 角色实体
 */
@Data
public class SysRole {

    private Long id;
    private String roleCode;
    private String roleName;
    private String description;
    private Integer deleted;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
