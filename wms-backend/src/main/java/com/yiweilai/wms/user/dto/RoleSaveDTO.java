package com.yiweilai.wms.user.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

/**
 * 角色保存 DTO
 */
@Data
public class RoleSaveDTO {
    private Long id;

    @NotBlank(message = "角色编码不能为空")
    private String roleCode;

    @NotBlank(message = "角色名称不能为空")
    private String roleName;

    private String description;

    /**
     * 权限ID列表
     */
    private List<Long> permissionIds;
}
