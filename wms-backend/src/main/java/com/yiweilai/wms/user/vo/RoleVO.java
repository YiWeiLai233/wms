package com.yiweilai.wms.user.vo;

import lombok.Data;

/**
 * 角色返回对象
 */
@Data
public class RoleVO {

    /** 角色ID */
    private Long id;

    /** 角色编码 */
    private String roleCode;

    /** 角色名称 */
    private String roleName;

    /** 描述 */
    private String description;
}
