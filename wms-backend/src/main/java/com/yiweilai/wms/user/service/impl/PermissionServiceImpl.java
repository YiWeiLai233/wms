package com.yiweilai.wms.user.service.impl;

import com.yiweilai.wms.user.entity.SysPermission;
import com.yiweilai.wms.user.mapper.PermissionMapper;
import com.yiweilai.wms.user.service.PermissionService;
import com.yiweilai.wms.user.vo.PermissionVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 权限 Service 实现
 */
@Service
@RequiredArgsConstructor
public class PermissionServiceImpl implements PermissionService {

    private final PermissionMapper permissionMapper;

    @Override
    public List<PermissionVO> getPermissionTree() {
        List<SysPermission> all = permissionMapper.selectAll();
        List<PermissionVO> voList = all.stream().map(this::convertToVO).collect(Collectors.toList());
        return buildTree(voList, 0L);
    }

    @Override
    public List<PermissionVO> getAllPermissions() {
        List<SysPermission> all = permissionMapper.selectAll();
        return all.stream().map(this::convertToVO).collect(Collectors.toList());
    }

    @Override
    public List<Long> getPermissionIdsByRoleId(Long roleId) {
        return permissionMapper.selectByRoleId(roleId).stream()
                .map(SysPermission::getId)
                .collect(Collectors.toList());
    }

    @Override
    public List<String> getPermissionCodesByUserId(Long userId) {
        return permissionMapper.selectCodesByUserId(userId);
    }

    private List<PermissionVO> buildTree(List<PermissionVO> all, Long parentId) {
        Map<Long, List<PermissionVO>> parentMap = all.stream()
                .collect(Collectors.groupingBy(PermissionVO::getParentId));
        return buildChildren(parentMap, parentId);
    }

    private List<PermissionVO> buildChildren(Map<Long, List<PermissionVO>> parentMap, Long parentId) {
        List<PermissionVO> children = parentMap.getOrDefault(parentId, new ArrayList<>());
        for (PermissionVO child : children) {
            child.setChildren(buildChildren(parentMap, child.getId()));
        }
        return children;
    }

    private PermissionVO convertToVO(SysPermission entity) {
        PermissionVO vo = new PermissionVO();
        vo.setId(entity.getId());
        vo.setPermissionCode(entity.getPermissionCode());
        vo.setPermissionName(entity.getPermissionName());
        vo.setParentId(entity.getParentId());
        vo.setType(entity.getType());
        vo.setSortOrder(entity.getSortOrder());
        return vo;
    }
}
