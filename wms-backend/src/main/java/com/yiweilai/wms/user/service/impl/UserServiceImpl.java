package com.yiweilai.wms.user.service.impl;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.yiweilai.wms.common.PageResult;
import com.yiweilai.wms.config.CacheService;
import com.yiweilai.wms.exception.BusinessException;
import com.yiweilai.wms.exception.ErrorCode;
import com.yiweilai.wms.security.JwtUtils;
import com.yiweilai.wms.user.dto.LoginRequest;
import com.yiweilai.wms.user.dto.LoginResponse;
import com.yiweilai.wms.user.dto.UserSaveDTO;
import com.yiweilai.wms.user.entity.SysRole;
import com.yiweilai.wms.user.entity.SysUser;
import com.yiweilai.wms.user.mapper.RoleMapper;
import com.yiweilai.wms.user.mapper.UserMapper;
import com.yiweilai.wms.user.service.UserService;
import com.yiweilai.wms.user.vo.RoleVO;
import com.yiweilai.wms.user.vo.UserVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 用户服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final RoleMapper roleMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;
    private final CacheService cacheService;

    private static final String CACHE_KEY_ROLES = "cache:roles:all";

    @Override
    public LoginResponse login(LoginRequest request) {
        // 查询用户
        SysUser user = userMapper.selectByUsername(request.getUsername());
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        // 校验密码
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BusinessException(ErrorCode.USER_PASSWORD_ERROR);
        }

        // 校验状态
        if (user.getStatus() != 1) {
            throw new BusinessException(ErrorCode.USER_DISABLED);
        }

        // 查询角色
        List<String> roles = userMapper.selectRoleCodesByUserId(user.getId());

        // 生成 Token
        String token = jwtUtils.generateToken(user.getId(), user.getUsername(), roles);

        // 清除该用户的旧登出时间戳（确保新 token 不会被旧的登出记录拒绝）
        cacheService.delete(JwtUtils.getUserLogoutKey(user.getId()));

        log.info("用户登录成功: username={}", user.getUsername());

        return LoginResponse.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .realName(user.getRealName())
                .token(token)
                .roles(roles)
                .build();
    }

    @Override
    public UserVO getCurrentUser(Long userId) {
        String cacheKey = "cache:user:" + userId;
        UserVO cached = cacheService.get(cacheKey);
        if (cached != null) {
            return cached;
        }

        SysUser user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        List<String> roles = userMapper.selectRoleCodesByUserId(userId);

        UserVO vo = new UserVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setRealName(user.getRealName());
        vo.setPhone(user.getPhone());
        vo.setEmail(user.getEmail());
        vo.setStatus(user.getStatus());
        vo.setRoles(roles);
        vo.setCreatedAt(user.getCreatedAt());

        cacheService.set(cacheKey, vo, 10, TimeUnit.MINUTES);
        return vo;
    }

    @Override
    public PageResult<UserVO> listUsers(String keyword, Integer pageNum, Integer pageSize) {
        PageHelper.startPage(pageNum, pageSize);
        List<SysUser> users = userMapper.selectList(keyword);
        PageInfo<SysUser> pageInfo = new PageInfo<>(users);

        List<UserVO> voList = users.stream().map(user -> {
            UserVO vo = new UserVO();
            vo.setId(user.getId());
            vo.setUsername(user.getUsername());
            vo.setRealName(user.getRealName());
            vo.setPhone(user.getPhone());
            vo.setEmail(user.getEmail());
            vo.setStatus(user.getStatus());
            vo.setCreatedAt(user.getCreatedAt());
            return vo;
        }).toList();

        return new PageResult<>(pageInfo.getTotal(), voList, pageNum, pageSize);
    }

    @Override
    public UserVO getById(Long id) {
        SysUser user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        List<String> roles = userMapper.selectRoleCodesByUserId(id);
        List<SysRole> roleEntities = roleMapper.selectByUserId(id);

        UserVO vo = new UserVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setRealName(user.getRealName());
        vo.setPhone(user.getPhone());
        vo.setEmail(user.getEmail());
        vo.setStatus(user.getStatus());
        vo.setRoles(roles);
        vo.setRoleIds(roleEntities.stream().map(SysRole::getId).collect(Collectors.toList()));
        vo.setCreatedAt(user.getCreatedAt());
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(UserSaveDTO dto) {
        // 检查用户名唯一性
        SysUser existing = userMapper.selectByUsername(dto.getUsername());
        if (existing != null) {
            throw new BusinessException(ErrorCode.USER_EXISTS);
        }

        // 创建用户
        SysUser user = new SysUser();
        user.setUsername(dto.getUsername());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setRealName(dto.getRealName());
        user.setPhone(dto.getPhone());
        user.setEmail(dto.getEmail());
        user.setStatus(dto.getStatus());
        userMapper.insert(user);

        // 分配角色
        if (dto.getRoleIds() != null && !dto.getRoleIds().isEmpty()) {
            for (Long roleId : dto.getRoleIds()) {
                userMapper.insertUserRole(user.getId(), roleId);
            }
        }

        return user.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(UserSaveDTO dto) {
        SysUser user = userMapper.selectById(dto.getId());
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        // 更新用户信息
        SysUser updateEntity = new SysUser();
        updateEntity.setId(dto.getId());
        updateEntity.setRealName(dto.getRealName());
        updateEntity.setPhone(dto.getPhone());
        updateEntity.setEmail(dto.getEmail());
        updateEntity.setStatus(dto.getStatus());
        userMapper.update(updateEntity);

        // 更新角色
        if (dto.getRoleIds() != null) {
            userMapper.deleteUserRoles(dto.getId());
            for (Long roleId : dto.getRoleIds()) {
                userMapper.insertUserRole(dto.getId(), roleId);
            }
        }

        cacheService.delete("cache:user:" + dto.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        SysUser user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        userMapper.deleteById(id);
        cacheService.delete("cache:user:" + id);
    }

    @Override
    public void resetPassword(Long userId, String newPassword) {
        SysUser user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        String encodedPassword = passwordEncoder.encode(newPassword);
        userMapper.updatePassword(userId, encodedPassword);
        cacheService.delete("cache:user:" + userId);
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<RoleVO> listRoles() {
        List<RoleVO> cached = cacheService.get(CACHE_KEY_ROLES);
        if (cached != null) {
            return cached;
        }
        List<SysRole> roles = roleMapper.selectAll();
        List<RoleVO> voList = roles.stream().map(role -> {
            RoleVO vo = new RoleVO();
            vo.setId(role.getId());
            vo.setRoleCode(role.getRoleCode());
            vo.setRoleName(role.getRoleName());
            vo.setDescription(role.getDescription());
            return vo;
        }).collect(Collectors.toList());
        cacheService.set(CACHE_KEY_ROLES, voList, 30, TimeUnit.MINUTES);
        return voList;
    }
}
