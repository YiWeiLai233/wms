package com.yiweilai.wms.user.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 角色权限关联 Mapper
 */
@Mapper
public interface RolePermissionMapper {

    /**
     * 根据角色ID查询权限ID列表
     */
    List<Long> selectPermissionIdsByRoleId(@Param("roleId") Long roleId);

    /**
     * 新增
     */
    int insert(@Param("roleId") Long roleId, @Param("permissionId") Long permissionId);

    /**
     * 删除角色的所有权限
     */
    int deleteByRoleId(@Param("roleId") Long roleId);

    /**
     * 批量新增
     */
    int batchInsert(@Param("roleId") Long roleId, @Param("permissionIds") List<Long> permissionIds);
}
