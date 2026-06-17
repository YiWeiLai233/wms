package com.yiweilai.wms.user.vo;

import lombok.Data;

import java.util.List;

/**
 * 权限 VO
 */
@Data
public class PermissionVO {
    private Long id;
    private String permissionCode;
    private String permissionName;
    private Long parentId;
    private Integer type;
    private Integer sortOrder;
    private List<PermissionVO> children;
}
