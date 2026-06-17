package com.yiweilai.wms.user.service;

import com.yiweilai.wms.user.dto.RoleSaveDTO;
import com.yiweilai.wms.user.vo.RoleVO;

import java.util.List;

/**
 * 角色 Service
 */
public interface RoleService {

    /**
     * 获取所有角色
     */
    List<RoleVO> listAll();

    /**
     * 根据ID获取角色
     */
    RoleVO getById(Long id);

    /**
     * 创建角色
     */
    Long create(RoleSaveDTO dto);

    /**
     * 更新角色
     */
    void update(RoleSaveDTO dto);

    /**
     * 删除角色
     */
    void delete(Long id);
}
