package com.yiweilai.wms.user.service;

import com.yiweilai.wms.user.vo.PermissionVO;

import java.util.List;

/**
 * 权限 Service
 */
public interface PermissionService {

    /**
     * 获取所有权限（树形）
     */
    List<PermissionVO> getPermissionTree();

    /**
     * 获取所有权限（扁平）
     */
    List<PermissionVO> getAllPermissions();

    /**
     * 根据角色ID获取权限ID列表
     */
    List<Long> getPermissionIdsByRoleId(Long roleId);

    /**
     * 根据用户ID获取权限编码列表
     */
    List<String> getPermissionCodesByUserId(Long userId);
}
