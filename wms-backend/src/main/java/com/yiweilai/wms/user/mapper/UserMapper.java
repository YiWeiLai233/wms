package com.yiweilai.wms.user.mapper;

import com.yiweilai.wms.user.entity.SysUser;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 用户 Mapper
 */
public interface UserMapper {

    /**
     * 根据用户名查询用户
     */
    SysUser selectByUsername(@Param("username") String username);

    /**
     * 根据ID查询用户
     */
    SysUser selectById(@Param("id") Long id);

    /**
     * 查询用户角色编码列表
     */
    List<String> selectRoleCodesByUserId(@Param("userId") Long userId);

    /**
     * 插入用户
     */
    int insert(SysUser user);

    /**
     * 更新用户
     */
    int update(SysUser user);

    /**
     * 分页查询用户列表
     */
    List<SysUser> selectList(@Param("keyword") String keyword);

    /**
     * 分配角色
     */
    int insertUserRole(@Param("userId") Long userId, @Param("roleId") Long roleId);

    /**
     * 删除用户角色
     */
    int deleteUserRoles(@Param("userId") Long userId);

    /**
     * 删除用户（逻辑删除）
     */
    int deleteById(@Param("id") Long id);

    /**
     * 更新密码
     */
    int updatePassword(@Param("id") Long id, @Param("password") String password);
}
