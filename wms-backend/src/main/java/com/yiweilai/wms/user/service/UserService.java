package com.yiweilai.wms.user.service;

import com.yiweilai.wms.common.PageResult;
import com.yiweilai.wms.user.dto.LoginRequest;
import com.yiweilai.wms.user.dto.LoginResponse;
import com.yiweilai.wms.user.dto.UserSaveDTO;
import com.yiweilai.wms.user.vo.RoleVO;
import com.yiweilai.wms.user.vo.UserVO;

import java.util.List;

/**
 * 用户服务接口
 */
public interface UserService {

    /**
     * 用户登录
     */
    LoginResponse login(LoginRequest request);

    /**
     * 获取当前用户信息
     */
    UserVO getCurrentUser(Long userId);

    /**
     * 分页查询用户列表
     */
    PageResult<UserVO> listUsers(String keyword, Integer pageNum, Integer pageSize);

    /**
     * 根据ID查询用户详情
     */
    UserVO getById(Long id);

    /**
     * 新增用户
     */
    Long create(UserSaveDTO dto);

    /**
     * 修改用户
     */
    void update(UserSaveDTO dto);

    /**
     * 删除用户
     */
    void delete(Long id);

    /**
     * 重置密码
     */
    void resetPassword(Long userId, String newPassword);

    /**
     * 获取所有角色列表
     */
    List<RoleVO> listRoles();
}
