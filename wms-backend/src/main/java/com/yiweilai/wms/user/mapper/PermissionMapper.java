package com.yiweilai.wms.user.mapper;

import com.yiweilai.wms.user.entity.SysPermission;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 权限 Mapper
 */
@Mapper
public interface PermissionMapper {

    /**
     * 查询所有权限
     */
    List<SysPermission> selectAll();

    /**
     * 根据ID查询
     */
    SysPermission selectById(@Param("id") Long id);

    /**
     * 根据权限编码查询
     */
    SysPermission selectByCode(@Param("code") String code);

    /**
     * 根据角色ID查询权限列表
     */
    List<SysPermission> selectByRoleId(@Param("roleId") Long roleId);

    /**
     * 根据用户ID查询权限编码列表
     */
    List<String> selectCodesByUserId(@Param("userId") Long userId);

    /**
     * 新增
     */
    int insert(SysPermission permission);

    /**
     * 更新
     */
    int update(SysPermission permission);

    /**
     * 删除
     */
    int deleteById(@Param("id") Long id);
}
