package com.yiweilai.wms.user.entity;

import lombok.Data;

/**
 * 角色权限关联实体
 */
@Data
public class SysRolePermission {
    private Long id;
    private Long roleId;
    private Long permissionId;
}
