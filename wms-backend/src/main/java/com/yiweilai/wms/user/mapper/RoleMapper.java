package com.yiweilai.wms.user.mapper;

import com.yiweilai.wms.user.entity.SysRole;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 角色 Mapper
 */
@Mapper
public interface RoleMapper {

    /**
     * 查询所有角色
     */
    List<SysRole> selectAll();

    /**
     * 根据ID查询角色
     */
    SysRole selectById(@Param("id") Long id);

    /**
     * 根据用户ID查询角色列表
     */
    List<SysRole> selectByUserId(@Param("userId") Long userId);

    /**
     * 新增角色
     */
    int insert(SysRole role);

    /**
     * 更新角色
     */
    int update(SysRole role);

    /**
     * 删除角色
     */
    int deleteById(@Param("id") Long id);
}
