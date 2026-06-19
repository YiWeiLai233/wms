package com.yiweilai.wms.user.service;

import com.yiweilai.wms.exception.BusinessException;
import com.yiweilai.wms.user.dto.RoleSaveDTO;
import com.yiweilai.wms.user.entity.SysRole;
import com.yiweilai.wms.user.mapper.RoleMapper;
import com.yiweilai.wms.user.mapper.RolePermissionMapper;
import com.yiweilai.wms.user.service.impl.RoleServiceImpl;
import com.yiweilai.wms.user.vo.RoleVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RoleServiceImplTest {

    @Mock
    private RoleMapper roleMapper;
    @Mock
    private RolePermissionMapper rolePermissionMapper;

    private RoleServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new RoleServiceImpl(roleMapper, rolePermissionMapper);
    }

    @Test
    void listAll_returnsAllRoles() {
        SysRole role1 = new SysRole();
        role1.setId(1L);
        role1.setRoleCode("ADMIN");
        role1.setRoleName("管理员");

        SysRole role2 = new SysRole();
        role2.setId(2L);
        role2.setRoleCode("USER");
        role2.setRoleName("普通用户");

        when(roleMapper.selectAll()).thenReturn(Arrays.asList(role1, role2));

        List<RoleVO> result = service.listAll();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getRoleCode()).isEqualTo("ADMIN");
        assertThat(result.get(1).getRoleCode()).isEqualTo("USER");
    }

    @Test
    void getById_existingRole_returnsRole() {
        SysRole role = new SysRole();
        role.setId(1L);
        role.setRoleCode("ADMIN");
        role.setRoleName("管理员");
        role.setDescription("系统管理员");

        when(roleMapper.selectById(1L)).thenReturn(role);

        RoleVO result = service.getById(1L);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getRoleCode()).isEqualTo("ADMIN");
        assertThat(result.getRoleName()).isEqualTo("管理员");
    }

    @Test
    void getById_notFound_throwsException() {
        when(roleMapper.selectById(999L)).thenReturn(null);

        assertThatThrownBy(() -> service.getById(999L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("角色不存在");
    }

    @Test
    void create_success() {
        RoleSaveDTO dto = new RoleSaveDTO();
        dto.setRoleCode("MANAGER");
        dto.setRoleName("经理");
        dto.setDescription("部门经理");
        dto.setPermissionIds(Arrays.asList(1L, 2L, 3L));

        when(roleMapper.insert(any(SysRole.class))).thenAnswer(invocation -> {
            SysRole role = invocation.getArgument(0);
            role.setId(10L);
            return 1;
        });

        Long roleId = service.create(dto);

        assertThat(roleId).isEqualTo(10L);

        ArgumentCaptor<SysRole> roleCaptor = ArgumentCaptor.forClass(SysRole.class);
        verify(roleMapper).insert(roleCaptor.capture());
        assertThat(roleCaptor.getValue().getRoleCode()).isEqualTo("MANAGER");

        verify(rolePermissionMapper).batchInsert(10L, Arrays.asList(1L, 2L, 3L));
    }

    @Test
    void create_noPermissions_skipsBatchInsert() {
        RoleSaveDTO dto = new RoleSaveDTO();
        dto.setRoleCode("VIEWER");
        dto.setRoleName("查看者");

        when(roleMapper.insert(any(SysRole.class))).thenAnswer(invocation -> {
            SysRole role = invocation.getArgument(0);
            role.setId(11L);
            return 1;
        });

        service.create(dto);

        verify(rolePermissionMapper, never()).batchInsert(any(), any());
    }

    @Test
    void update_success() {
        SysRole existingRole = new SysRole();
        existingRole.setId(1L);
        existingRole.setRoleCode("OLD_CODE");
        existingRole.setRoleName("旧名称");

        RoleSaveDTO dto = new RoleSaveDTO();
        dto.setId(1L);
        dto.setRoleCode("NEW_CODE");
        dto.setRoleName("新名称");
        dto.setDescription("新描述");
        dto.setPermissionIds(Arrays.asList(4L, 5L));

        when(roleMapper.selectById(1L)).thenReturn(existingRole);

        service.update(dto);

        ArgumentCaptor<SysRole> roleCaptor = ArgumentCaptor.forClass(SysRole.class);
        verify(roleMapper).update(roleCaptor.capture());
        assertThat(roleCaptor.getValue().getRoleCode()).isEqualTo("NEW_CODE");
        assertThat(roleCaptor.getValue().getRoleName()).isEqualTo("新名称");

        verify(rolePermissionMapper).deleteByRoleId(1L);
        verify(rolePermissionMapper).batchInsert(1L, Arrays.asList(4L, 5L));
    }

    @Test
    void update_notFound_throwsException() {
        RoleSaveDTO dto = new RoleSaveDTO();
        dto.setId(999L);

        when(roleMapper.selectById(999L)).thenReturn(null);

        assertThatThrownBy(() -> service.update(dto))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("角色不存在");
    }

    @Test
    void delete_success() {
        SysRole role = new SysRole();
        role.setId(1L);
        role.setRoleCode("ADMIN");

        when(roleMapper.selectById(1L)).thenReturn(role);

        service.delete(1L);

        verify(roleMapper).deleteById(1L);
        verify(rolePermissionMapper).deleteByRoleId(1L);
    }

    @Test
    void delete_notFound_throwsException() {
        when(roleMapper.selectById(999L)).thenReturn(null);

        assertThatThrownBy(() -> service.delete(999L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("角色不存在");
    }
}
