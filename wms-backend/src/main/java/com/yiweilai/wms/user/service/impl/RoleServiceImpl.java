package com.yiweilai.wms.user.service.impl;

import com.yiweilai.wms.exception.BusinessException;
import com.yiweilai.wms.exception.ErrorCode;
import com.yiweilai.wms.user.dto.RoleSaveDTO;
import com.yiweilai.wms.user.entity.SysRole;
import com.yiweilai.wms.user.mapper.RoleMapper;
import com.yiweilai.wms.user.mapper.RolePermissionMapper;
import com.yiweilai.wms.user.service.RoleService;
import com.yiweilai.wms.user.vo.RoleVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 角色 Service 实现
 */
@Service
@RequiredArgsConstructor
public class RoleServiceImpl implements RoleService {

    private final RoleMapper roleMapper;
    private final RolePermissionMapper rolePermissionMapper;

    @Override
    public List<RoleVO> listAll() {
        return roleMapper.selectAll().stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
    }

    @Override
    public RoleVO getById(Long id) {
        SysRole role = roleMapper.selectById(id);
        if (role == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "角色不存在");
        }
        return convertToVO(role);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(RoleSaveDTO dto) {
        // 检查编码唯一性
        // TODO: 可以添加唯一性检查

        SysRole role = new SysRole();
        role.setRoleCode(dto.getRoleCode());
        role.setRoleName(dto.getRoleName());
        role.setDescription(dto.getDescription());
        roleMapper.insert(role);

        // 保存角色权限关联
        if (dto.getPermissionIds() != null && !dto.getPermissionIds().isEmpty()) {
            rolePermissionMapper.batchInsert(role.getId(), dto.getPermissionIds());
        }

        return role.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(RoleSaveDTO dto) {
        SysRole role = roleMapper.selectById(dto.getId());
        if (role == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "角色不存在");
        }

        role.setRoleCode(dto.getRoleCode());
        role.setRoleName(dto.getRoleName());
        role.setDescription(dto.getDescription());
        roleMapper.update(role);

        // 更新角色权限关联
        rolePermissionMapper.deleteByRoleId(dto.getId());
        if (dto.getPermissionIds() != null && !dto.getPermissionIds().isEmpty()) {
            rolePermissionMapper.batchInsert(dto.getId(), dto.getPermissionIds());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        SysRole role = roleMapper.selectById(id);
        if (role == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "角色不存在");
        }
        roleMapper.deleteById(id);
        rolePermissionMapper.deleteByRoleId(id);
    }

    private RoleVO convertToVO(SysRole entity) {
        RoleVO vo = new RoleVO();
        vo.setId(entity.getId());
        vo.setRoleCode(entity.getRoleCode());
        vo.setRoleName(entity.getRoleName());
        vo.setDescription(entity.getDescription());
        return vo;
    }
}
