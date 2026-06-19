package com.yiweilai.wms.user.service;

import com.yiweilai.wms.config.CacheService;
import com.yiweilai.wms.exception.BusinessException;
import com.yiweilai.wms.security.JwtUtils;
import com.yiweilai.wms.user.dto.LoginRequest;
import com.yiweilai.wms.user.dto.LoginResponse;
import com.yiweilai.wms.user.entity.SysUser;
import com.yiweilai.wms.user.mapper.RoleMapper;
import com.yiweilai.wms.user.mapper.UserMapper;
import com.yiweilai.wms.user.service.impl.UserServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserMapper userMapper;
    @Mock
    private RoleMapper roleMapper;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtUtils jwtUtils;
    @Mock
    private CacheService cacheService;

    private UserServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new UserServiceImpl(userMapper, roleMapper, passwordEncoder, jwtUtils, cacheService);
    }

    @Test
    void login_success() {
        LoginRequest request = new LoginRequest();
        request.setUsername("admin");
        request.setPassword("123456");

        SysUser user = new SysUser();
        user.setId(1L);
        user.setUsername("admin");
        user.setPassword("encoded_password");
        user.setRealName("管理员");
        user.setStatus(1);

        when(userMapper.selectByUsername("admin")).thenReturn(user);
        when(passwordEncoder.matches("123456", "encoded_password")).thenReturn(true);
        when(userMapper.selectRoleCodesByUserId(1L)).thenReturn(List.of("SUPER_ADMIN"));
        when(jwtUtils.generateToken(1L, "admin", List.of("SUPER_ADMIN"))).thenReturn("token123");

        LoginResponse response = service.login(request);

        assertThat(response.getUserId()).isEqualTo(1L);
        assertThat(response.getUsername()).isEqualTo("admin");
        assertThat(response.getToken()).isEqualTo("token123");
        assertThat(response.getRoles()).containsExactly("SUPER_ADMIN");
    }

    @Test
    void login_userNotFound_throwsException() {
        LoginRequest request = new LoginRequest();
        request.setUsername("nonexistent");
        request.setPassword("123456");

        when(userMapper.selectByUsername("nonexistent")).thenReturn(null);

        assertThatThrownBy(() -> service.login(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("用户不存在");
    }

    @Test
    void login_wrongPassword_throwsException() {
        LoginRequest request = new LoginRequest();
        request.setUsername("admin");
        request.setPassword("wrong_password");

        SysUser user = new SysUser();
        user.setId(1L);
        user.setUsername("admin");
        user.setPassword("encoded_password");
        user.setStatus(1);

        when(userMapper.selectByUsername("admin")).thenReturn(user);
        when(passwordEncoder.matches("wrong_password", "encoded_password")).thenReturn(false);

        assertThatThrownBy(() -> service.login(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("密码错误");
    }

    @Test
    void login_disabledUser_throwsException() {
        LoginRequest request = new LoginRequest();
        request.setUsername("admin");
        request.setPassword("123456");

        SysUser user = new SysUser();
        user.setId(1L);
        user.setUsername("admin");
        user.setPassword("encoded_password");
        user.setStatus(0); // 禁用

        when(userMapper.selectByUsername("admin")).thenReturn(user);
        when(passwordEncoder.matches("123456", "encoded_password")).thenReturn(true);

        assertThatThrownBy(() -> service.login(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("已禁用");
    }

    @Test
    void delete_success() {
        SysUser user = new SysUser();
        user.setId(1L);
        user.setUsername("admin");

        when(userMapper.selectById(1L)).thenReturn(user);

        service.delete(1L);

        verify(userMapper).deleteById(1L);
        verify(cacheService).delete("cache:user:1");
    }

    @Test
    void delete_userNotFound_throwsException() {
        when(userMapper.selectById(999L)).thenReturn(null);

        assertThatThrownBy(() -> service.delete(999L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("用户不存在");
    }

    @Test
    void resetPassword_success() {
        SysUser user = new SysUser();
        user.setId(1L);
        user.setUsername("admin");

        when(userMapper.selectById(1L)).thenReturn(user);
        when(passwordEncoder.encode("new_password")).thenReturn("encoded_new_password");

        service.resetPassword(1L, "new_password");

        verify(userMapper).updatePassword(1L, "encoded_new_password");
        verify(cacheService).delete("cache:user:1");
    }

    @Test
    void resetPassword_userNotFound_throwsException() {
        when(userMapper.selectById(999L)).thenReturn(null);

        assertThatThrownBy(() -> service.resetPassword(999L, "new_password"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("用户不存在");
    }
}
