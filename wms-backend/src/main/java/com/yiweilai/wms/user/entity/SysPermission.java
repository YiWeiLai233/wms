package com.yiweilai.wms.user.entity;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 系统权限实体
 */
@Data
public class SysPermission {
    private Long id;
    private String permissionCode;
    private String permissionName;
    private Long parentId;
    private Integer type;       // 1=菜单, 2=按钮
    private Integer sortOrder;
    private Integer deleted;
    private LocalDateTime createdAt;
}
