# Custom Role Permissions Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build custom role templates with module/action permissions, backend 403 enforcement, and frontend menu/route/button filtering.

**Architecture:** Use the existing `sys_role`, `sys_permission`, `sys_user_role`, and `sys_role_permission` tables. Backend services expose role and permission APIs, a Spring MVC interceptor checks path-to-permission rules on every protected API request, and the Vue app uses effective permission codes from login/profile responses to filter UI.

**Tech Stack:** Java 17, Spring Boot 4, Spring Security, Spring MVC interceptor, MyBatis XML mappers, JUnit 5, Mockito, Vue 3, Pinia, Vue Router, Element Plus, TypeScript.

---

## File Structure

Backend files to create:

- `wms-backend/src/main/java/com/yiweilai/wms/user/entity/SysPermission.java`: maps `sys_permission`.
- `wms-backend/src/main/java/com/yiweilai/wms/user/dto/RoleSaveDTO.java`: role create/update request.
- `wms-backend/src/main/java/com/yiweilai/wms/user/dto/RolePermissionUpdateDTO.java`: role permission replacement request.
- `wms-backend/src/main/java/com/yiweilai/wms/user/vo/PermissionVO.java`: permission tree node returned to the frontend.
- `wms-backend/src/main/java/com/yiweilai/wms/user/mapper/PermissionMapper.java`: permission and role-permission database access.
- `wms-backend/src/main/java/com/yiweilai/wms/user/service/PermissionService.java`: permission query/check contract.
- `wms-backend/src/main/java/com/yiweilai/wms/user/service/RoleService.java`: custom role management contract.
- `wms-backend/src/main/java/com/yiweilai/wms/user/service/impl/PermissionServiceImpl.java`: permission tree, effective permission, and all-permission logic.
- `wms-backend/src/main/java/com/yiweilai/wms/user/service/impl/RoleServiceImpl.java`: custom role CRUD and safety rules.
- `wms-backend/src/main/java/com/yiweilai/wms/user/controller/RoleController.java`: `/api/roles` and `/api/permissions/tree`.
- `wms-backend/src/main/java/com/yiweilai/wms/security/PermissionRouteRule.java`: immutable route rule.
- `wms-backend/src/main/java/com/yiweilai/wms/security/PermissionRouteRegistry.java`: centralized API route-to-permission catalog.
- `wms-backend/src/main/java/com/yiweilai/wms/security/PermissionInterceptor.java`: backend permission guard.
- `wms-backend/src/main/resources/mapper/PermissionMapper.xml`: SQL for permissions.
- `wms-backend/sql/V20260616__custom_role_permissions.sql`: seed permissions and default role mappings.
- `wms-backend/src/test/java/com/yiweilai/wms/user/service/PermissionServiceImplTest.java`: permission service tests.
- `wms-backend/src/test/java/com/yiweilai/wms/user/service/RoleServiceImplTest.java`: role service tests.
- `wms-backend/src/test/java/com/yiweilai/wms/security/PermissionInterceptorTest.java`: 403 enforcement tests.

Backend files to modify:

- `wms-backend/src/main/java/com/yiweilai/wms/user/entity/SysRole.java`: no new column; built-in roles are recognized by role code.
- `wms-backend/src/main/java/com/yiweilai/wms/user/vo/RoleVO.java`: add `permissionIds`.
- `wms-backend/src/main/java/com/yiweilai/wms/user/vo/UserVO.java`: add `permissions`.
- `wms-backend/src/main/java/com/yiweilai/wms/user/dto/LoginResponse.java`: add `permissions`.
- `wms-backend/src/main/java/com/yiweilai/wms/user/mapper/RoleMapper.java`: add insert/update/delete/count methods.
- `wms-backend/src/main/resources/mapper/RoleMapper.xml`: add SQL for role CRUD and safety checks.
- `wms-backend/src/main/java/com/yiweilai/wms/user/service/UserService.java`: remove role-list responsibility or leave unused; user APIs should rely on `RoleService`.
- `wms-backend/src/main/java/com/yiweilai/wms/user/service/impl/UserServiceImpl.java`: include permissions in login/profile/user details and protect dangerous user updates.
- `wms-backend/src/main/java/com/yiweilai/wms/user/controller/UserController.java`: remove duplicate `/api/roles`, use request attributes for current user safety.
- `wms-backend/src/main/java/com/yiweilai/wms/common/config/WebMvcConfig.java`: register the permission interceptor.
- `wms-backend/src/main/java/com/yiweilai/wms/exception/GlobalExceptionHandler.java`: return HTTP 403 for forbidden business responses if needed.

Frontend files to create:

- `wsm-web/src/views/system/RoleList.vue`: role template management page.

Frontend files to modify:

- `wsm-web/src/api/auth.ts`: type permissions on login/profile.
- `wsm-web/src/api/user.ts`: add role CRUD and permission APIs.
- `wsm-web/src/stores/user.ts`: store effective permission codes.
- `wsm-web/src/composables/usePermission.ts`: add permission helpers.
- `wsm-web/src/utils/constants.ts`: add permission fields to menu items and add role menu item.
- `wsm-web/src/router/index.ts`: add route permission metadata and guard unauthorized access.
- `wsm-web/src/layouts/DefaultLayout.vue`: render filtered menu.
- `wsm-web/src/views/system/UserList.vue`: hide user actions by permission and support custom role names.
- Any page touched for operation buttons: hide or disable action buttons according to the operation permissions listed below.

---

## Permission Code Map

Use this exact initial permission catalog:

```text
dashboard:view
ai:assistant
ai:knowledge
warehouse:view
warehouse:create
warehouse:update
warehouse:delete
product:view
product:create
product:update
product:delete
stock:view
stock:adjust
stock:check
stock:log
order:view
order:import
order:update
outbound:view
outbound:create
outbound:scan
outbound:confirm
outbound:cancel
return:view
return:create
return:check
return:confirm
return:cancel
express:view
express:company
express:template
express:report
system:user
system:role
system:file
system:log
system:stock-alert
system:stock-alert-template
system:platform
```

Backend route enforcement uses these rules:

```text
GET    /api/auth/profile                         authenticated only
ALL    /api/users/**                             system:user
ALL    /api/roles/**                             system:role
ALL    /api/permissions/**                       system:role
GET    /api/reports/dashboard                    dashboard:view
GET    /api/reports/stock                        stock:view
GET    /api/reports/outbound                     outbound:view
GET    /api/reports/express-fee                  express:report
ALL    /api/ai/knowledge/**                      ai:knowledge
ALL    /api/ai/chat                              ai:assistant
ALL    /api/ai/conversations/**                  ai:assistant
ALL    /api/ai/actions/**                        ai:assistant
ALL    /api/products/**                          product:view/create/update/delete by method
ALL    /api/skus/**                              product:view/create/update/delete by method
ALL    /api/categories/**                        product:view/create/update/delete by method
ALL    /api/warehouses/**                        warehouse:view/create/update/delete by method
ALL    /api/warehouse-areas/**                   warehouse:view/create/update/delete by method
ALL    /api/warehouse-shelves/**                 warehouse:view/create/update/delete by method
GET    /api/stocks/query                         stock:view
GET    /api/stocks/special                       stock:view
POST   /api/stocks/adjust                        stock:adjust
POST   /api/stocks/confirm-sellable              stock:adjust
POST   /api/stocks/confirm-dispose               stock:adjust
POST   /api/stocks/confirm-scrap                 stock:adjust
GET    /api/stock-logs/**                        stock:log
ALL    /api/stock-checks/**                      stock:check
GET    /api/orders                               order:view
GET    /api/orders/**                            order:view
POST   /api/orders/import                        order:import
POST   /api/orders/import-file                   order:import
PUT    /api/orders/**                            order:update
GET    /api/orders/search/**                     order:view
GET    /api/outbound/**                          outbound:view
POST   /api/outbound/create                      outbound:create
POST   /api/outbound/create-batch                outbound:create
POST   /api/outbound/scan                        outbound:scan
POST   /api/outbound/confirm                     outbound:confirm
POST   /api/outbound/*/cancel                    outbound:cancel
PUT    /api/outbound/**                          outbound:create
GET    /api/returns/**                           return:view
POST   /api/returns/create                       return:create
POST   /api/returns/create-batch                 return:create
POST   /api/returns/check                        return:check
POST   /api/returns/confirm                      return:confirm
POST   /api/returns/*/cancel                     return:cancel
POST   /api/returns/cancel-by-order/**           return:cancel
GET    /api/express/query                        express:view
POST   /api/express/calculate-fee                express:view
ALL    /api/express/companies/**                 express:company
ALL    /api/express/fee-templates/**             express:template
ALL    /api/operation-logs/**                    system:log
ALL    /api/files/**                             system:file
ALL    /api/images/**                            system:file
ALL    /api/privacy/**                           system:role
ALL    /api/platforms/**                         system:platform
ALL    /api/stock-alert-configs/**               system:stock-alert
ALL    /api/stock-alert-templates/**             system:stock-alert-template
ALL    /api/ocr/**                               outbound:scan
```

Public and token-special paths remain excluded:

```text
/api/health
/api/auth/login
/api/auth/register
/api/ai/internal/**
/api/ai/tools/**
POST /api/ai/actions/pending with X-AI-Service-Token
/swagger-ui/**
/v3/api-docs/**
/doc.html
/webjars/**
/images/**
OPTIONS /**
```

---

### Task 1: Backend Permission DTOs, VOs, and Entity

**Files:**
- Create: `wms-backend/src/main/java/com/yiweilai/wms/user/entity/SysPermission.java`
- Create: `wms-backend/src/main/java/com/yiweilai/wms/user/dto/RoleSaveDTO.java`
- Create: `wms-backend/src/main/java/com/yiweilai/wms/user/dto/RolePermissionUpdateDTO.java`
- Create: `wms-backend/src/main/java/com/yiweilai/wms/user/vo/PermissionVO.java`
- Modify: `wms-backend/src/main/java/com/yiweilai/wms/user/vo/RoleVO.java`
- Modify: `wms-backend/src/main/java/com/yiweilai/wms/user/vo/UserVO.java`
- Modify: `wms-backend/src/main/java/com/yiweilai/wms/user/dto/LoginResponse.java`

- [ ] **Step 1: Add the permission entity**

Create `SysPermission.java`:

```java
package com.yiweilai.wms.user.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SysPermission {

    private Long id;
    private String permissionCode;
    private String permissionName;
    private Long parentId;
    private Integer type;
    private Integer sortOrder;
    private Integer deleted;
    private LocalDateTime createdAt;
}
```

- [ ] **Step 2: Add role request DTOs**

Create `RoleSaveDTO.java`:

```java
package com.yiweilai.wms.user.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

@Data
public class RoleSaveDTO {

    @NotBlank(message = "角色编码不能为空")
    private String roleCode;

    @NotBlank(message = "角色名称不能为空")
    private String roleName;

    private String description;
    private List<Long> permissionIds;
}
```

Create `RolePermissionUpdateDTO.java`:

```java
package com.yiweilai.wms.user.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class RolePermissionUpdateDTO {

    private List<Long> permissionIds = new ArrayList<>();
}
```

- [ ] **Step 3: Add permission tree VO**

Create `PermissionVO.java`:

```java
package com.yiweilai.wms.user.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class PermissionVO {

    private Long id;
    private String permissionCode;
    private String permissionName;
    private Long parentId;
    private Integer type;
    private Integer sortOrder;
    private List<PermissionVO> children = new ArrayList<>();
}
```

- [ ] **Step 4: Extend response models**

Add these fields to `RoleVO.java`:

```java
private List<Long> permissionIds;
private List<String> permissionCodes;
```

Add this field to `UserVO.java`:

```java
private List<String> permissions;
```

Add this field to `LoginResponse.java`:

```java
private List<String> permissions;
```

- [ ] **Step 5: Compile-check the DTO layer**

Run:

```powershell
cd wms-backend
.\mvnw.cmd -DskipTests compile
```

Expected: compilation fails only if imports for `List` are missing in the modified VO/DTO files. Add `import java.util.List;` to any modified file that needs it.

- [ ] **Step 6: Commit**

Run:

```powershell
git add wms-backend/src/main/java/com/yiweilai/wms/user/entity/SysPermission.java `
  wms-backend/src/main/java/com/yiweilai/wms/user/dto/RoleSaveDTO.java `
  wms-backend/src/main/java/com/yiweilai/wms/user/dto/RolePermissionUpdateDTO.java `
  wms-backend/src/main/java/com/yiweilai/wms/user/vo/PermissionVO.java `
  wms-backend/src/main/java/com/yiweilai/wms/user/vo/RoleVO.java `
  wms-backend/src/main/java/com/yiweilai/wms/user/vo/UserVO.java `
  wms-backend/src/main/java/com/yiweilai/wms/user/dto/LoginResponse.java
git commit -m "feat: add permission response models"
```

---

### Task 2: Backend Mappers and SQL XML

**Files:**
- Create: `wms-backend/src/main/java/com/yiweilai/wms/user/mapper/PermissionMapper.java`
- Create: `wms-backend/src/main/resources/mapper/PermissionMapper.xml`
- Modify: `wms-backend/src/main/java/com/yiweilai/wms/user/mapper/RoleMapper.java`
- Modify: `wms-backend/src/main/resources/mapper/RoleMapper.xml`
- Modify: `wms-backend/src/main/java/com/yiweilai/wms/user/mapper/UserMapper.java`
- Modify: `wms-backend/src/main/resources/mapper/user/UserMapper.xml`

- [ ] **Step 1: Add the permission mapper interface**

Create `PermissionMapper.java`:

```java
package com.yiweilai.wms.user.mapper;

import com.yiweilai.wms.user.entity.SysPermission;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface PermissionMapper {

    List<SysPermission> selectAll();

    List<Long> selectPermissionIdsByRoleId(@Param("roleId") Long roleId);

    List<String> selectPermissionCodesByRoleId(@Param("roleId") Long roleId);

    List<String> selectPermissionCodesByUserId(@Param("userId") Long userId);

    int deleteRolePermissions(@Param("roleId") Long roleId);

    int insertRolePermission(@Param("roleId") Long roleId, @Param("permissionId") Long permissionId);

    int countExistingPermissionIds(@Param("permissionIds") List<Long> permissionIds);
}
```

- [ ] **Step 2: Add PermissionMapper XML**

Create `PermissionMapper.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN" "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="com.yiweilai.wms.user.mapper.PermissionMapper">

    <resultMap id="BaseResultMap" type="com.yiweilai.wms.user.entity.SysPermission">
        <id column="id" property="id"/>
        <result column="permission_code" property="permissionCode"/>
        <result column="permission_name" property="permissionName"/>
        <result column="parent_id" property="parentId"/>
        <result column="type" property="type"/>
        <result column="sort_order" property="sortOrder"/>
        <result column="deleted" property="deleted"/>
        <result column="created_at" property="createdAt"/>
    </resultMap>

    <select id="selectAll" resultMap="BaseResultMap">
        SELECT id, permission_code, permission_name, parent_id, type, sort_order, deleted, created_at
        FROM sys_permission
        WHERE deleted = 0
        ORDER BY parent_id ASC, sort_order ASC, id ASC
    </select>

    <select id="selectPermissionIdsByRoleId" resultType="long">
        SELECT p.id
        FROM sys_permission p
        INNER JOIN sys_role_permission rp ON rp.permission_id = p.id
        WHERE rp.role_id = #{roleId}
          AND p.deleted = 0
        ORDER BY p.parent_id ASC, p.sort_order ASC, p.id ASC
    </select>

    <select id="selectPermissionCodesByRoleId" resultType="string">
        SELECT p.permission_code
        FROM sys_permission p
        INNER JOIN sys_role_permission rp ON rp.permission_id = p.id
        WHERE rp.role_id = #{roleId}
          AND p.deleted = 0
        ORDER BY p.parent_id ASC, p.sort_order ASC, p.id ASC
    </select>

    <select id="selectPermissionCodesByUserId" resultType="string">
        SELECT DISTINCT p.permission_code
        FROM sys_permission p
        INNER JOIN sys_role_permission rp ON rp.permission_id = p.id
        INNER JOIN sys_user_role ur ON ur.role_id = rp.role_id
        INNER JOIN sys_role r ON r.id = ur.role_id
        WHERE ur.user_id = #{userId}
          AND p.deleted = 0
          AND r.deleted = 0
        ORDER BY p.permission_code ASC
    </select>

    <delete id="deleteRolePermissions">
        DELETE FROM sys_role_permission WHERE role_id = #{roleId}
    </delete>

    <insert id="insertRolePermission">
        INSERT INTO sys_role_permission (role_id, permission_id)
        VALUES (#{roleId}, #{permissionId})
    </insert>

    <select id="countExistingPermissionIds" resultType="int">
        SELECT COUNT(1)
        FROM sys_permission
        WHERE deleted = 0
          AND id IN
          <foreach collection="permissionIds" item="id" open="(" separator="," close=")">
              #{id}
          </foreach>
    </select>
</mapper>
```

- [ ] **Step 3: Extend RoleMapper**

Add these methods to `RoleMapper.java`:

```java
SysRole selectByCode(@Param("roleCode") String roleCode);

int insert(SysRole role);

int update(SysRole role);

int deleteById(@Param("id") Long id);

int countUsersByRoleId(@Param("roleId") Long roleId);
```

Add these SQL statements to `RoleMapper.xml`:

```xml
<select id="selectByCode" resultMap="BaseResultMap">
    SELECT id, role_code, role_name, description, deleted, created_at, updated_at
    FROM sys_role
    WHERE role_code = #{roleCode} AND deleted = 0
</select>

<insert id="insert" useGeneratedKeys="true" keyProperty="id">
    INSERT INTO sys_role (role_code, role_name, description)
    VALUES (#{roleCode}, #{roleName}, #{description})
</insert>

<update id="update">
    UPDATE sys_role
    <set>
        <if test="roleName != null">role_name = #{roleName},</if>
        <if test="description != null">description = #{description},</if>
    </set>
    WHERE id = #{id} AND deleted = 0
</update>

<update id="deleteById">
    UPDATE sys_role SET deleted = 1 WHERE id = #{id} AND deleted = 0
</update>

<select id="countUsersByRoleId" resultType="int">
    SELECT COUNT(1)
    FROM sys_user_role ur
    INNER JOIN sys_user u ON u.id = ur.user_id
    WHERE ur.role_id = #{roleId}
      AND u.deleted = 0
</select>
```

- [ ] **Step 4: Add user mapper safety helpers**

Add these methods to `UserMapper.java`:

```java
int countEnabledUsersByRoleCode(@Param("roleCode") String roleCode);

int countEnabledUsersByRoleCodeExcludingUser(@Param("roleCode") String roleCode, @Param("userId") Long userId);
```

Add these SQL statements to `UserMapper.xml`:

```xml
<select id="countEnabledUsersByRoleCode" resultType="int">
    SELECT COUNT(1)
    FROM sys_user u
    INNER JOIN sys_user_role ur ON ur.user_id = u.id
    INNER JOIN sys_role r ON r.id = ur.role_id
    WHERE r.role_code = #{roleCode}
      AND r.deleted = 0
      AND u.deleted = 0
      AND u.status = 1
</select>

<select id="countEnabledUsersByRoleCodeExcludingUser" resultType="int">
    SELECT COUNT(1)
    FROM sys_user u
    INNER JOIN sys_user_role ur ON ur.user_id = u.id
    INNER JOIN sys_role r ON r.id = ur.role_id
    WHERE r.role_code = #{roleCode}
      AND r.deleted = 0
      AND u.deleted = 0
      AND u.status = 1
      AND u.id != #{userId}
</select>
```

- [ ] **Step 5: Run mapper compile check**

Run:

```powershell
cd wms-backend
.\mvnw.cmd -DskipTests compile
```

Expected: compile succeeds. Mapper XML syntax is checked later by tests.

- [ ] **Step 6: Commit**

Run:

```powershell
git add wms-backend/src/main/java/com/yiweilai/wms/user/mapper/PermissionMapper.java `
  wms-backend/src/main/resources/mapper/PermissionMapper.xml `
  wms-backend/src/main/java/com/yiweilai/wms/user/mapper/RoleMapper.java `
  wms-backend/src/main/resources/mapper/RoleMapper.xml `
  wms-backend/src/main/java/com/yiweilai/wms/user/mapper/UserMapper.java `
  wms-backend/src/main/resources/mapper/user/UserMapper.xml
git commit -m "feat: add role permission mappers"
```

---

### Task 3: Permission Service with TDD

**Files:**
- Create: `wms-backend/src/test/java/com/yiweilai/wms/user/service/PermissionServiceImplTest.java`
- Create: `wms-backend/src/main/java/com/yiweilai/wms/user/service/PermissionService.java`
- Create: `wms-backend/src/main/java/com/yiweilai/wms/user/service/impl/PermissionServiceImpl.java`

- [ ] **Step 1: Write failing permission service tests**

Create `PermissionServiceImplTest.java`:

```java
package com.yiweilai.wms.user.service;

import com.yiweilai.wms.user.entity.SysPermission;
import com.yiweilai.wms.user.mapper.PermissionMapper;
import com.yiweilai.wms.user.mapper.UserMapper;
import com.yiweilai.wms.user.service.impl.PermissionServiceImpl;
import com.yiweilai.wms.user.vo.PermissionVO;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PermissionServiceImplTest {

    @Test
    void listPermissionTreeGroupsChildrenUnderParentModules() {
        PermissionMapper permissionMapper = mock(PermissionMapper.class);
        UserMapper userMapper = mock(UserMapper.class);
        PermissionService service = new PermissionServiceImpl(permissionMapper, userMapper);
        when(permissionMapper.selectAll()).thenReturn(List.of(
                permission(1L, "product", "商品管理", 0L, 1, 10),
                permission(2L, "product:view", "商品查看", 1L, 2, 11),
                permission(3L, "product:create", "商品新增", 1L, 2, 12)
        ));

        List<PermissionVO> tree = service.listPermissionTree();

        assertThat(tree).hasSize(1);
        assertThat(tree.get(0).getPermissionCode()).isEqualTo("product");
        assertThat(tree.get(0).getChildren())
                .extracting(PermissionVO::getPermissionCode)
                .containsExactly("product:view", "product:create");
    }

    @Test
    void superAdminHasAnyPermissionWithoutRolePermissionRows() {
        PermissionMapper permissionMapper = mock(PermissionMapper.class);
        UserMapper userMapper = mock(UserMapper.class);
        PermissionService service = new PermissionServiceImpl(permissionMapper, userMapper);
        when(userMapper.selectRoleCodesByUserId(1L)).thenReturn(List.of("SUPER_ADMIN"));

        assertThat(service.hasPermission(1L, "system:role")).isTrue();
    }

    @Test
    void normalUserChecksEffectivePermissionCodes() {
        PermissionMapper permissionMapper = mock(PermissionMapper.class);
        UserMapper userMapper = mock(UserMapper.class);
        PermissionService service = new PermissionServiceImpl(permissionMapper, userMapper);
        when(userMapper.selectRoleCodesByUserId(7L)).thenReturn(List.of("OPERATOR"));
        when(permissionMapper.selectPermissionCodesByUserId(7L)).thenReturn(List.of("stock:view", "outbound:scan"));

        assertThat(service.hasPermission(7L, "outbound:scan")).isTrue();
        assertThat(service.hasPermission(7L, "system:role")).isFalse();
        assertThat(service.getEffectivePermissionCodes(7L)).containsExactly("stock:view", "outbound:scan");
    }

    private SysPermission permission(Long id, String code, String name, Long parentId, Integer type, Integer sortOrder) {
        SysPermission permission = new SysPermission();
        permission.setId(id);
        permission.setPermissionCode(code);
        permission.setPermissionName(name);
        permission.setParentId(parentId);
        permission.setType(type);
        permission.setSortOrder(sortOrder);
        permission.setDeleted(0);
        return permission;
    }
}
```

- [ ] **Step 2: Run the failing tests**

Run:

```powershell
cd wms-backend
.\mvnw.cmd -Dtest=PermissionServiceImplTest test
```

Expected: FAIL because `PermissionService` and `PermissionServiceImpl` do not exist yet.

- [ ] **Step 3: Add the service contract**

Create `PermissionService.java`:

```java
package com.yiweilai.wms.user.service;

import com.yiweilai.wms.user.vo.PermissionVO;

import java.util.List;

public interface PermissionService {

    List<PermissionVO> listPermissionTree();

    List<String> getEffectivePermissionCodes(Long userId);

    boolean hasPermission(Long userId, String permissionCode);

    void replaceRolePermissions(Long roleId, List<Long> permissionIds);
}
```

- [ ] **Step 4: Implement the service**

Create `PermissionServiceImpl.java`:

```java
package com.yiweilai.wms.user.service.impl;

import com.yiweilai.wms.exception.BusinessException;
import com.yiweilai.wms.exception.ErrorCode;
import com.yiweilai.wms.user.entity.SysPermission;
import com.yiweilai.wms.user.mapper.PermissionMapper;
import com.yiweilai.wms.user.mapper.UserMapper;
import com.yiweilai.wms.user.service.PermissionService;
import com.yiweilai.wms.user.vo.PermissionVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PermissionServiceImpl implements PermissionService {

    private static final String SUPER_ADMIN = "SUPER_ADMIN";

    private final PermissionMapper permissionMapper;
    private final UserMapper userMapper;

    @Override
    public List<PermissionVO> listPermissionTree() {
        List<SysPermission> permissions = permissionMapper.selectAll();
        Map<Long, PermissionVO> nodeMap = new LinkedHashMap<>();
        List<PermissionVO> roots = new ArrayList<>();

        for (SysPermission permission : permissions) {
            nodeMap.put(permission.getId(), toVO(permission));
        }

        for (SysPermission permission : permissions) {
            PermissionVO node = nodeMap.get(permission.getId());
            Long parentId = permission.getParentId() == null ? 0L : permission.getParentId();
            if (parentId == 0L || !nodeMap.containsKey(parentId)) {
                roots.add(node);
            } else {
                nodeMap.get(parentId).getChildren().add(node);
            }
        }

        return roots;
    }

    @Override
    public List<String> getEffectivePermissionCodes(Long userId) {
        List<String> roleCodes = userMapper.selectRoleCodesByUserId(userId);
        if (roleCodes != null && roleCodes.contains(SUPER_ADMIN)) {
            return permissionMapper.selectAll().stream()
                    .map(SysPermission::getPermissionCode)
                    .toList();
        }
        return permissionMapper.selectPermissionCodesByUserId(userId);
    }

    @Override
    public boolean hasPermission(Long userId, String permissionCode) {
        if (userId == null || permissionCode == null || permissionCode.isBlank()) {
            return false;
        }
        List<String> roleCodes = userMapper.selectRoleCodesByUserId(userId);
        if (roleCodes != null && roleCodes.contains(SUPER_ADMIN)) {
            return true;
        }
        return permissionMapper.selectPermissionCodesByUserId(userId).contains(permissionCode);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void replaceRolePermissions(Long roleId, List<Long> permissionIds) {
        List<Long> safeIds = permissionIds == null ? List.of() : permissionIds.stream().distinct().toList();
        if (!safeIds.isEmpty()) {
            int existing = permissionMapper.countExistingPermissionIds(safeIds);
            if (existing != safeIds.size()) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "权限不存在");
            }
        }

        permissionMapper.deleteRolePermissions(roleId);
        for (Long permissionId : safeIds) {
            permissionMapper.insertRolePermission(roleId, permissionId);
        }
    }

    private PermissionVO toVO(SysPermission permission) {
        PermissionVO vo = new PermissionVO();
        vo.setId(permission.getId());
        vo.setPermissionCode(permission.getPermissionCode());
        vo.setPermissionName(permission.getPermissionName());
        vo.setParentId(permission.getParentId());
        vo.setType(permission.getType());
        vo.setSortOrder(permission.getSortOrder());
        return vo;
    }
}
```

If the compiler reports an unused import in this file, remove only the unused import.

- [ ] **Step 5: Run the permission service tests**

Run:

```powershell
cd wms-backend
.\mvnw.cmd -Dtest=PermissionServiceImplTest test
```

Expected: PASS.

- [ ] **Step 6: Commit**

Run:

```powershell
git add wms-backend/src/test/java/com/yiweilai/wms/user/service/PermissionServiceImplTest.java `
  wms-backend/src/main/java/com/yiweilai/wms/user/service/PermissionService.java `
  wms-backend/src/main/java/com/yiweilai/wms/user/service/impl/PermissionServiceImpl.java
git commit -m "feat: add permission service"
```

---

### Task 4: Role Service with Custom Role Safety Rules

**Files:**
- Create: `wms-backend/src/test/java/com/yiweilai/wms/user/service/RoleServiceImplTest.java`
- Create: `wms-backend/src/main/java/com/yiweilai/wms/user/service/RoleService.java`
- Create: `wms-backend/src/main/java/com/yiweilai/wms/user/service/impl/RoleServiceImpl.java`

- [ ] **Step 1: Write failing role service tests**

Create `RoleServiceImplTest.java`:

```java
package com.yiweilai.wms.user.service;

import com.yiweilai.wms.exception.BusinessException;
import com.yiweilai.wms.user.dto.RoleSaveDTO;
import com.yiweilai.wms.user.entity.SysRole;
import com.yiweilai.wms.user.mapper.PermissionMapper;
import com.yiweilai.wms.user.mapper.RoleMapper;
import com.yiweilai.wms.user.service.impl.RoleServiceImpl;
import com.yiweilai.wms.user.vo.RoleVO;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RoleServiceImplTest {

    @Test
    void createCustomRoleStoresRoleAndPermissions() {
        RoleMapper roleMapper = mock(RoleMapper.class);
        PermissionMapper permissionMapper = mock(PermissionMapper.class);
        PermissionService permissionService = mock(PermissionService.class);
        RoleService service = new RoleServiceImpl(roleMapper, permissionMapper, permissionService);
        RoleSaveDTO dto = new RoleSaveDTO();
        dto.setRoleCode("CUSTOMER_SERVICE");
        dto.setRoleName("客服");
        dto.setDescription("处理订单咨询");
        dto.setPermissionIds(List.of(10L, 11L));
        when(roleMapper.selectByCode("CUSTOMER_SERVICE")).thenReturn(null);
        when(roleMapper.insert(any(SysRole.class))).thenAnswer(invocation -> {
            SysRole role = invocation.getArgument(0);
            role.setId(9L);
            return 1;
        });

        Long roleId = service.create(dto);

        assertThat(roleId).isEqualTo(9L);
        verify(permissionService).replaceRolePermissions(9L, List.of(10L, 11L));
    }

    @Test
    void deleteBuiltInRoleIsRejected() {
        RoleMapper roleMapper = mock(RoleMapper.class);
        PermissionMapper permissionMapper = mock(PermissionMapper.class);
        PermissionService permissionService = mock(PermissionService.class);
        RoleService service = new RoleServiceImpl(roleMapper, permissionMapper, permissionService);
        SysRole role = role(1L, "SUPER_ADMIN", "超级管理员");
        when(roleMapper.selectById(1L)).thenReturn(role);

        assertThatThrownBy(() -> service.delete(1L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("内置角色");

        verify(roleMapper, never()).deleteById(1L);
    }

    @Test
    void listRolesIncludesPermissionIdsAndCodes() {
        RoleMapper roleMapper = mock(RoleMapper.class);
        PermissionMapper permissionMapper = mock(PermissionMapper.class);
        PermissionService permissionService = mock(PermissionService.class);
        RoleService service = new RoleServiceImpl(roleMapper, permissionMapper, permissionService);
        when(roleMapper.selectAll()).thenReturn(List.of(role(2L, "OPERATOR", "操作员")));
        when(permissionMapper.selectPermissionIdsByRoleId(2L)).thenReturn(List.of(5L, 6L));
        when(permissionMapper.selectPermissionCodesByRoleId(2L)).thenReturn(List.of("stock:view", "outbound:scan"));

        List<RoleVO> roles = service.listRoles();

        assertThat(roles).hasSize(1);
        assertThat(roles.get(0).getPermissionIds()).containsExactly(5L, 6L);
        assertThat(roles.get(0).getPermissionCodes()).containsExactly("stock:view", "outbound:scan");
    }

    private SysRole role(Long id, String code, String name) {
        SysRole role = new SysRole();
        role.setId(id);
        role.setRoleCode(code);
        role.setRoleName(name);
        role.setDescription(name);
        role.setDeleted(0);
        return role;
    }
}
```

- [ ] **Step 2: Run the failing role tests**

Run:

```powershell
cd wms-backend
.\mvnw.cmd -Dtest=RoleServiceImplTest test
```

Expected: FAIL because `RoleService` and `RoleServiceImpl` do not exist yet.

- [ ] **Step 3: Add the role service contract**

Create `RoleService.java`:

```java
package com.yiweilai.wms.user.service;

import com.yiweilai.wms.user.dto.RoleSaveDTO;
import com.yiweilai.wms.user.vo.RoleVO;

import java.util.List;

public interface RoleService {

    List<RoleVO> listRoles();

    RoleVO getById(Long id);

    Long create(RoleSaveDTO dto);

    void update(Long id, RoleSaveDTO dto);

    void delete(Long id);

    void updatePermissions(Long id, List<Long> permissionIds);
}
```

- [ ] **Step 4: Implement role service**

Create `RoleServiceImpl.java`:

```java
package com.yiweilai.wms.user.service.impl;

import com.yiweilai.wms.exception.BusinessException;
import com.yiweilai.wms.exception.ErrorCode;
import com.yiweilai.wms.user.dto.RoleSaveDTO;
import com.yiweilai.wms.user.entity.SysRole;
import com.yiweilai.wms.user.mapper.PermissionMapper;
import com.yiweilai.wms.user.mapper.RoleMapper;
import com.yiweilai.wms.user.service.PermissionService;
import com.yiweilai.wms.user.service.RoleService;
import com.yiweilai.wms.user.vo.RoleVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class RoleServiceImpl implements RoleService {

    private static final Set<String> BUILT_IN_ROLE_CODES = Set.of(
            "SUPER_ADMIN", "WAREHOUSE_ADMIN", "OPERATOR", "VIEWER");

    private final RoleMapper roleMapper;
    private final PermissionMapper permissionMapper;
    private final PermissionService permissionService;

    @Override
    public List<RoleVO> listRoles() {
        return roleMapper.selectAll().stream().map(this::toVO).toList();
    }

    @Override
    public RoleVO getById(Long id) {
        SysRole role = roleMapper.selectById(id);
        if (role == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "角色不存在");
        }
        return toVO(role);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(RoleSaveDTO dto) {
        if (roleMapper.selectByCode(dto.getRoleCode()) != null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "角色编码已存在");
        }
        SysRole role = new SysRole();
        role.setRoleCode(dto.getRoleCode());
        role.setRoleName(dto.getRoleName());
        role.setDescription(dto.getDescription());
        roleMapper.insert(role);
        permissionService.replaceRolePermissions(role.getId(), dto.getPermissionIds());
        return role.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, RoleSaveDTO dto) {
        SysRole existing = requireRole(id);
        SysRole role = new SysRole();
        role.setId(id);
        role.setRoleName(dto.getRoleName());
        role.setDescription(dto.getDescription());
        roleMapper.update(role);
        if (dto.getPermissionIds() != null) {
            permissionService.replaceRolePermissions(existing.getId(), dto.getPermissionIds());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        SysRole role = requireRole(id);
        if (BUILT_IN_ROLE_CODES.contains(role.getRoleCode())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "内置角色不能删除");
        }
        if (roleMapper.countUsersByRoleId(id) > 0) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "角色已分配用户，不能删除");
        }
        permissionMapper.deleteRolePermissions(id);
        roleMapper.deleteById(id);
    }

    @Override
    public void updatePermissions(Long id, List<Long> permissionIds) {
        requireRole(id);
        permissionService.replaceRolePermissions(id, permissionIds);
    }

    private SysRole requireRole(Long id) {
        SysRole role = roleMapper.selectById(id);
        if (role == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "角色不存在");
        }
        return role;
    }

    private RoleVO toVO(SysRole role) {
        RoleVO vo = new RoleVO();
        vo.setId(role.getId());
        vo.setRoleCode(role.getRoleCode());
        vo.setRoleName(role.getRoleName());
        vo.setDescription(role.getDescription());
        vo.setPermissionIds(permissionMapper.selectPermissionIdsByRoleId(role.getId()));
        vo.setPermissionCodes(permissionMapper.selectPermissionCodesByRoleId(role.getId()));
        return vo;
    }
}
```

- [ ] **Step 5: Run role service tests**

Run:

```powershell
cd wms-backend
.\mvnw.cmd -Dtest=RoleServiceImplTest test
```

Expected: PASS.

- [ ] **Step 6: Commit**

Run:

```powershell
git add wms-backend/src/test/java/com/yiweilai/wms/user/service/RoleServiceImplTest.java `
  wms-backend/src/main/java/com/yiweilai/wms/user/service/RoleService.java `
  wms-backend/src/main/java/com/yiweilai/wms/user/service/impl/RoleServiceImpl.java
git commit -m "feat: add custom role service"
```

---

### Task 5: Backend Permission Interceptor

**Files:**
- Create: `wms-backend/src/test/java/com/yiweilai/wms/security/PermissionInterceptorTest.java`
- Create: `wms-backend/src/main/java/com/yiweilai/wms/security/PermissionRouteRule.java`
- Create: `wms-backend/src/main/java/com/yiweilai/wms/security/PermissionRouteRegistry.java`
- Create: `wms-backend/src/main/java/com/yiweilai/wms/security/PermissionInterceptor.java`
- Modify: `wms-backend/src/main/java/com/yiweilai/wms/common/config/WebMvcConfig.java`

- [ ] **Step 1: Write failing interceptor tests**

Create `PermissionInterceptorTest.java`:

```java
package com.yiweilai.wms.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yiweilai.wms.user.service.PermissionService;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PermissionInterceptorTest {

    @Test
    void requestWithPermissionPasses() throws Exception {
        PermissionService permissionService = mock(PermissionService.class);
        PermissionInterceptor interceptor = new PermissionInterceptor(
                permissionService, new PermissionRouteRegistry(), new ObjectMapper());
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/products");
        request.setAttribute("userId", 7L);
        MockHttpServletResponse response = new MockHttpServletResponse();
        when(permissionService.hasPermission(7L, "product:view")).thenReturn(true);

        boolean allowed = interceptor.preHandle(request, response, new Object());

        assertThat(allowed).isTrue();
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    void requestWithoutPermissionReturns403() throws Exception {
        PermissionService permissionService = mock(PermissionService.class);
        PermissionInterceptor interceptor = new PermissionInterceptor(
                permissionService, new PermissionRouteRegistry(), new ObjectMapper());
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/products");
        request.setAttribute("userId", 7L);
        MockHttpServletResponse response = new MockHttpServletResponse();
        when(permissionService.hasPermission(7L, "product:create")).thenReturn(false);

        boolean allowed = interceptor.preHandle(request, response, new Object());

        assertThat(allowed).isFalse();
        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getContentAsString()).contains("无权限访问");
    }

    @Test
    void authenticatedProfilePassesWithoutPermissionCode() throws Exception {
        PermissionService permissionService = mock(PermissionService.class);
        PermissionInterceptor interceptor = new PermissionInterceptor(
                permissionService, new PermissionRouteRegistry(), new ObjectMapper());
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/auth/profile");
        request.setAttribute("userId", 7L);
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean allowed = interceptor.preHandle(request, response, new Object());

        assertThat(allowed).isTrue();
    }
}
```

- [ ] **Step 2: Run the failing interceptor tests**

Run:

```powershell
cd wms-backend
.\mvnw.cmd -Dtest=PermissionInterceptorTest test
```

Expected: FAIL because the interceptor classes do not exist yet.

- [ ] **Step 3: Add route rule model**

Create `PermissionRouteRule.java`:

```java
package com.yiweilai.wms.security;

import org.springframework.http.HttpMethod;
import org.springframework.util.AntPathMatcher;

public record PermissionRouteRule(HttpMethod method, String pattern, String permissionCode) {

    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    public boolean matches(String requestMethod, String path) {
        boolean methodMatches = method == null || method.name().equalsIgnoreCase(requestMethod);
        return methodMatches && PATH_MATCHER.match(pattern, path);
    }
}
```

- [ ] **Step 4: Add route registry**

Create `PermissionRouteRegistry.java` with explicit rule ordering:

```java
package com.yiweilai.wms.security;

import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class PermissionRouteRegistry {

    private static final List<String> AUTHENTICATED_ONLY = List.of(
            "/api/auth/profile"
    );

    private static final List<PermissionRouteRule> RULES = List.of(
            rule(HttpMethod.GET, "/api/reports/dashboard", "dashboard:view"),
            rule(HttpMethod.GET, "/api/reports/stock", "stock:view"),
            rule(HttpMethod.GET, "/api/reports/outbound", "outbound:view"),
            rule(HttpMethod.GET, "/api/reports/express-fee", "express:report"),
            rule(null, "/api/users/**", "system:user"),
            rule(null, "/api/roles/**", "system:role"),
            rule(null, "/api/permissions/**", "system:role"),
            rule(null, "/api/ai/knowledge/**", "ai:knowledge"),
            rule(null, "/api/ai/chat", "ai:assistant"),
            rule(null, "/api/ai/conversations/**", "ai:assistant"),
            rule(null, "/api/ai/actions/**", "ai:assistant"),
            rule(HttpMethod.POST, "/api/products/**", "product:create"),
            rule(HttpMethod.PUT, "/api/products/**", "product:update"),
            rule(HttpMethod.DELETE, "/api/products/**", "product:delete"),
            rule(HttpMethod.GET, "/api/products/**", "product:view"),
            rule(HttpMethod.POST, "/api/skus/**", "product:create"),
            rule(HttpMethod.PUT, "/api/skus/**", "product:update"),
            rule(HttpMethod.DELETE, "/api/skus/**", "product:delete"),
            rule(HttpMethod.GET, "/api/skus/**", "product:view"),
            rule(HttpMethod.POST, "/api/categories/**", "product:create"),
            rule(HttpMethod.PUT, "/api/categories/**", "product:update"),
            rule(HttpMethod.DELETE, "/api/categories/**", "product:delete"),
            rule(HttpMethod.GET, "/api/categories/**", "product:view"),
            rule(HttpMethod.POST, "/api/warehouses/**", "warehouse:create"),
            rule(HttpMethod.PUT, "/api/warehouses/**", "warehouse:update"),
            rule(HttpMethod.DELETE, "/api/warehouses/**", "warehouse:delete"),
            rule(HttpMethod.GET, "/api/warehouses/**", "warehouse:view"),
            rule(HttpMethod.POST, "/api/warehouse-areas/**", "warehouse:create"),
            rule(HttpMethod.PUT, "/api/warehouse-areas/**", "warehouse:update"),
            rule(HttpMethod.DELETE, "/api/warehouse-areas/**", "warehouse:delete"),
            rule(HttpMethod.GET, "/api/warehouse-areas/**", "warehouse:view"),
            rule(HttpMethod.POST, "/api/warehouse-shelves/**", "warehouse:create"),
            rule(HttpMethod.PUT, "/api/warehouse-shelves/**", "warehouse:update"),
            rule(HttpMethod.DELETE, "/api/warehouse-shelves/**", "warehouse:delete"),
            rule(HttpMethod.GET, "/api/warehouse-shelves/**", "warehouse:view"),
            rule(HttpMethod.GET, "/api/stocks/query", "stock:view"),
            rule(HttpMethod.GET, "/api/stocks/special", "stock:view"),
            rule(HttpMethod.POST, "/api/stocks/adjust", "stock:adjust"),
            rule(HttpMethod.POST, "/api/stocks/confirm-sellable", "stock:adjust"),
            rule(HttpMethod.POST, "/api/stocks/confirm-dispose", "stock:adjust"),
            rule(HttpMethod.POST, "/api/stocks/confirm-scrap", "stock:adjust"),
            rule(HttpMethod.GET, "/api/stock-logs/**", "stock:log"),
            rule(null, "/api/stock-checks/**", "stock:check"),
            rule(HttpMethod.POST, "/api/orders/import", "order:import"),
            rule(HttpMethod.POST, "/api/orders/import-file", "order:import"),
            rule(HttpMethod.PUT, "/api/orders/**", "order:update"),
            rule(HttpMethod.GET, "/api/orders/search/**", "order:view"),
            rule(HttpMethod.GET, "/api/orders/**", "order:view"),
            rule(HttpMethod.GET, "/api/orders", "order:view"),
            rule(HttpMethod.POST, "/api/outbound/create", "outbound:create"),
            rule(HttpMethod.POST, "/api/outbound/create-batch", "outbound:create"),
            rule(HttpMethod.POST, "/api/outbound/scan", "outbound:scan"),
            rule(HttpMethod.POST, "/api/outbound/confirm", "outbound:confirm"),
            rule(HttpMethod.POST, "/api/outbound/*/cancel", "outbound:cancel"),
            rule(HttpMethod.PUT, "/api/outbound/**", "outbound:create"),
            rule(HttpMethod.GET, "/api/outbound/**", "outbound:view"),
            rule(HttpMethod.POST, "/api/returns/create", "return:create"),
            rule(HttpMethod.POST, "/api/returns/create-batch", "return:create"),
            rule(HttpMethod.POST, "/api/returns/check", "return:check"),
            rule(HttpMethod.POST, "/api/returns/confirm", "return:confirm"),
            rule(HttpMethod.POST, "/api/returns/*/cancel", "return:cancel"),
            rule(HttpMethod.POST, "/api/returns/cancel-by-order/**", "return:cancel"),
            rule(HttpMethod.GET, "/api/returns/**", "return:view"),
            rule(HttpMethod.GET, "/api/express/query", "express:view"),
            rule(HttpMethod.POST, "/api/express/calculate-fee", "express:view"),
            rule(null, "/api/express/companies/**", "express:company"),
            rule(null, "/api/express/fee-templates/**", "express:template"),
            rule(null, "/api/operation-logs/**", "system:log"),
            rule(null, "/api/files/**", "system:file"),
            rule(null, "/api/images/**", "system:file"),
            rule(null, "/api/privacy/**", "system:role"),
            rule(null, "/api/platforms/**", "system:platform"),
            rule(null, "/api/stock-alert-configs/**", "system:stock-alert"),
            rule(null, "/api/stock-alert-templates/**", "system:stock-alert-template"),
            rule(null, "/api/ocr/**", "outbound:scan")
    );

    public boolean isAuthenticatedOnly(String path) {
        return AUTHENTICATED_ONLY.stream().anyMatch(path::equals);
    }

    public Optional<String> findPermission(String method, String path) {
        return RULES.stream()
                .filter(rule -> rule.matches(method, path))
                .map(PermissionRouteRule::permissionCode)
                .findFirst();
    }

    private static PermissionRouteRule rule(HttpMethod method, String pattern, String permissionCode) {
        return new PermissionRouteRule(method, pattern, permissionCode);
    }
}
```

- [ ] **Step 5: Add interceptor**

Create `PermissionInterceptor.java`:

```java
package com.yiweilai.wms.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yiweilai.wms.common.Result;
import com.yiweilai.wms.user.service.PermissionService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class PermissionInterceptor implements HandlerInterceptor {

    private final PermissionService permissionService;
    private final PermissionRouteRegistry routeRegistry;
    private final ObjectMapper objectMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (HttpMethod.OPTIONS.matches(request.getMethod())) {
            return true;
        }

        String path = request.getRequestURI();
        if (routeRegistry.isAuthenticatedOnly(path)) {
            return true;
        }

        Long userId = getUserId(request);
        if (userId == null) {
            writeForbidden(response, "无权限访问");
            return false;
        }

        String permissionCode = routeRegistry.findPermission(request.getMethod(), path).orElse(null);
        if (permissionCode == null) {
            if (path.startsWith("/api/")) {
                writeForbidden(response, "未配置权限规则");
                return false;
            }
            return true;
        }

        if (!permissionService.hasPermission(userId, permissionCode)) {
            writeForbidden(response, "无权限访问");
            return false;
        }

        return true;
    }

    private Long getUserId(HttpServletRequest request) {
        Object userId = request.getAttribute("userId");
        if (userId instanceof Long value) {
            return value;
        }
        if (userId instanceof Number value) {
            return value.longValue();
        }
        return null;
    }

    private void writeForbidden(HttpServletResponse response, String message) throws IOException {
        response.setStatus(403);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(Result.error(403, message)));
    }
}
```

- [ ] **Step 6: Register interceptor**

Modify `WebMvcConfig.java`:

```java
private final com.yiweilai.wms.security.PermissionInterceptor permissionInterceptor;

public WebMvcConfig(com.yiweilai.wms.security.PermissionInterceptor permissionInterceptor) {
    this.permissionInterceptor = permissionInterceptor;
}

@Override
public void addInterceptors(org.springframework.web.servlet.config.annotation.InterceptorRegistry registry) {
    registry.addInterceptor(permissionInterceptor)
            .addPathPatterns("/api/**")
            .excludePathPatterns(
                    "/api/health",
                    "/api/auth/login",
                    "/api/auth/register",
                    "/api/ai/internal/**",
                    "/api/ai/tools/**",
                    "/swagger-ui/**",
                    "/v3/api-docs/**",
                    "/doc.html",
                    "/webjars/**");
}
```

Keep the existing `addResourceHandlers` and `addCorsMappings` methods unchanged.

- [ ] **Step 7: Run interceptor tests**

Run:

```powershell
cd wms-backend
.\mvnw.cmd -Dtest=PermissionInterceptorTest test
```

Expected: PASS.

- [ ] **Step 8: Commit**

Run:

```powershell
git add wms-backend/src/test/java/com/yiweilai/wms/security/PermissionInterceptorTest.java `
  wms-backend/src/main/java/com/yiweilai/wms/security/PermissionRouteRule.java `
  wms-backend/src/main/java/com/yiweilai/wms/security/PermissionRouteRegistry.java `
  wms-backend/src/main/java/com/yiweilai/wms/security/PermissionInterceptor.java `
  wms-backend/src/main/java/com/yiweilai/wms/common/config/WebMvcConfig.java
git commit -m "feat: enforce backend permissions"
```

---

### Task 6: Role and Permission APIs

**Files:**
- Create: `wms-backend/src/main/java/com/yiweilai/wms/user/controller/RoleController.java`
- Modify: `wms-backend/src/main/java/com/yiweilai/wms/user/controller/UserController.java`
- Modify: `wms-backend/src/main/java/com/yiweilai/wms/user/service/UserService.java`
- Modify: `wms-backend/src/main/java/com/yiweilai/wms/user/service/impl/UserServiceImpl.java`

- [ ] **Step 1: Add RoleController**

Create `RoleController.java`:

```java
package com.yiweilai.wms.user.controller;

import com.yiweilai.wms.common.Result;
import com.yiweilai.wms.exception.BusinessException;
import com.yiweilai.wms.exception.ErrorCode;
import com.yiweilai.wms.user.dto.RolePermissionUpdateDTO;
import com.yiweilai.wms.user.dto.RoleSaveDTO;
import com.yiweilai.wms.user.service.PermissionService;
import com.yiweilai.wms.user.service.RoleService;
import com.yiweilai.wms.user.vo.PermissionVO;
import com.yiweilai.wms.user.vo.RoleVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collection;
import java.util.List;

@Tag(name = "角色权限管理", description = "自定义角色和权限模板")
@RestController
@RequiredArgsConstructor
public class RoleController {

    private final RoleService roleService;
    private final PermissionService permissionService;

    @Operation(summary = "角色列表")
    @GetMapping("/api/roles")
    public Result<List<RoleVO>> listRoles() {
        return Result.success(roleService.listRoles());
    }

    @Operation(summary = "角色详情")
    @GetMapping("/api/roles/{id}")
    public Result<RoleVO> getRole(@PathVariable Long id) {
        return Result.success(roleService.getById(id));
    }

    @Operation(summary = "新增角色")
    @PostMapping("/api/roles")
    public Result<Long> createRole(@Valid @RequestBody RoleSaveDTO dto,
                                   @RequestAttribute(value = "roles", required = false) List<String> roles) {
        requireSuperAdmin(roles);
        return Result.success(roleService.create(dto));
    }

    @Operation(summary = "修改角色")
    @PutMapping("/api/roles/{id}")
    public Result<Void> updateRole(@PathVariable Long id,
                                   @Valid @RequestBody RoleSaveDTO dto,
                                   @RequestAttribute(value = "roles", required = false) List<String> roles) {
        requireSuperAdmin(roles);
        roleService.update(id, dto);
        return Result.success();
    }

    @Operation(summary = "删除角色")
    @DeleteMapping("/api/roles/{id}")
    public Result<Void> deleteRole(@PathVariable Long id,
                                   @RequestAttribute(value = "roles", required = false) List<String> roles) {
        requireSuperAdmin(roles);
        roleService.delete(id);
        return Result.success();
    }

    @Operation(summary = "权限树")
    @GetMapping("/api/permissions/tree")
    public Result<List<PermissionVO>> listPermissionTree(@RequestAttribute(value = "roles", required = false) List<String> roles) {
        requireSuperAdmin(roles);
        return Result.success(permissionService.listPermissionTree());
    }

    @Operation(summary = "修改角色权限")
    @PutMapping("/api/roles/{id}/permissions")
    public Result<Void> updateRolePermissions(@PathVariable Long id,
                                               @RequestBody RolePermissionUpdateDTO dto,
                                               @RequestAttribute(value = "roles", required = false) List<String> roles) {
        requireSuperAdmin(roles);
        roleService.updatePermissions(id, dto.getPermissionIds());
        return Result.success();
    }

    private void requireSuperAdmin(Collection<String> roles) {
        if (roles == null || !roles.contains("SUPER_ADMIN")) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "只有超级管理员可以维护角色权限");
        }
    }
}
```

- [ ] **Step 2: Remove duplicate role endpoint from UserController**

In `UserController.java`, delete the `listRoles()` method and the unused imports for `RoleVO` and `List`.

- [ ] **Step 3: Include permissions in login/profile**

Modify `UserServiceImpl` constructor dependencies to include `PermissionService permissionService`.

In `login`, after loading `roles`, add:

```java
List<String> permissions = permissionService.getEffectivePermissionCodes(user.getId());
```

Add `.permissions(permissions)` to the `LoginResponse.builder()`.

In `getCurrentUser`, after `vo.setRoles(roles);`, add:

```java
vo.setPermissions(permissionService.getEffectivePermissionCodes(userId));
```

In `getById`, after `vo.setRoles(roles);`, add:

```java
vo.setPermissions(permissionService.getEffectivePermissionCodes(id));
```

- [ ] **Step 4: Add user safety rules**

In `UserServiceImpl.update`, before applying `userMapper.update(updateEntity);`, reject disabling the last enabled super admin:

```java
if (Integer.valueOf(0).equals(dto.getStatus())) {
    List<String> currentRoles = userMapper.selectRoleCodesByUserId(dto.getId());
    if (currentRoles.contains("SUPER_ADMIN")
            && userMapper.countEnabledUsersByRoleCodeExcludingUser("SUPER_ADMIN", dto.getId()) == 0) {
        throw new BusinessException(ErrorCode.BAD_REQUEST, "不能禁用最后一个超级管理员");
    }
}
```

Before replacing user roles, reject removing `SUPER_ADMIN` from the last enabled super admin:

```java
if (dto.getRoleIds() != null) {
    List<String> currentRoles = userMapper.selectRoleCodesByUserId(dto.getId());
    boolean isCurrentSuperAdmin = currentRoles.contains("SUPER_ADMIN");
    boolean hasOtherSuperAdmin = userMapper.countEnabledUsersByRoleCodeExcludingUser("SUPER_ADMIN", dto.getId()) > 0;
    boolean keepsSuperAdmin = dto.getRoleIds().stream()
            .map(roleMapper::selectById)
            .filter(java.util.Objects::nonNull)
            .anyMatch(role -> "SUPER_ADMIN".equals(role.getRoleCode()));
    if (isCurrentSuperAdmin && !keepsSuperAdmin && !hasOtherSuperAdmin) {
        throw new BusinessException(ErrorCode.BAD_REQUEST, "不能移除最后一个超级管理员角色");
    }
}
```

In `delete`, before `userMapper.deleteById(id);`, reject deleting the last enabled super admin:

```java
List<String> roles = userMapper.selectRoleCodesByUserId(id);
if (roles.contains("SUPER_ADMIN")
        && userMapper.countEnabledUsersByRoleCodeExcludingUser("SUPER_ADMIN", id) == 0) {
    throw new BusinessException(ErrorCode.BAD_REQUEST, "不能删除最后一个超级管理员");
}
```

- [ ] **Step 5: Compile the API layer**

Run:

```powershell
cd wms-backend
.\mvnw.cmd -DskipTests compile
```

Expected: PASS after removing `listRoles()` from `UserService` and `UserServiceImpl`, because role listing now belongs to `RoleService`.

- [ ] **Step 6: Run backend service tests**

Run:

```powershell
cd wms-backend
.\mvnw.cmd -Dtest=PermissionServiceImplTest,RoleServiceImplTest,PermissionInterceptorTest,JwtAuthFilterTest test
```

Expected: PASS.

- [ ] **Step 7: Commit**

Run:

```powershell
git add wms-backend/src/main/java/com/yiweilai/wms/user/controller/RoleController.java `
  wms-backend/src/main/java/com/yiweilai/wms/user/controller/UserController.java `
  wms-backend/src/main/java/com/yiweilai/wms/user/service/UserService.java `
  wms-backend/src/main/java/com/yiweilai/wms/user/service/impl/UserServiceImpl.java
git commit -m "feat: expose role permission APIs"
```

---

### Task 7: SQL Permission Seed

**Files:**
- Create: `wms-backend/sql/V20260616__custom_role_permissions.sql`

- [ ] **Step 1: Add seed SQL**

Create `V20260616__custom_role_permissions.sql` with parent module rows and action rows. Use MySQL `INSERT IGNORE` so reruns are safe:

```sql
-- Custom role permissions seed

INSERT IGNORE INTO sys_permission (permission_code, permission_name, parent_id, type, sort_order)
VALUES
('dashboard', '仪表盘', 0, 1, 10),
('ai', '智能助手', 0, 1, 20),
('warehouse', '仓库管理', 0, 1, 30),
('product', '商品管理', 0, 1, 40),
('stock', '库存管理', 0, 1, 50),
('order', '订单管理', 0, 1, 60),
('outbound', '发货管理', 0, 1, 70),
('return', '退货管理', 0, 1, 80),
('express', '快递管理', 0, 1, 90),
('system', '系统管理', 0, 1, 100);

INSERT IGNORE INTO sys_permission (permission_code, permission_name, parent_id, type, sort_order)
SELECT 'dashboard:view', '查看仪表盘', id, 2, 11 FROM sys_permission WHERE permission_code = 'dashboard';
INSERT IGNORE INTO sys_permission (permission_code, permission_name, parent_id, type, sort_order)
SELECT 'ai:assistant', 'AI助手', id, 2, 21 FROM sys_permission WHERE permission_code = 'ai';
INSERT IGNORE INTO sys_permission (permission_code, permission_name, parent_id, type, sort_order)
SELECT 'ai:knowledge', '知识库管理', id, 2, 22 FROM sys_permission WHERE permission_code = 'ai';

INSERT IGNORE INTO sys_permission (permission_code, permission_name, parent_id, type, sort_order)
SELECT code, name, parent.id, 2, sort_order
FROM sys_permission parent
JOIN (
    SELECT 'warehouse:view' code, '仓库查看' name, 31 sort_order UNION ALL
    SELECT 'warehouse:create', '仓库新增', 32 UNION ALL
    SELECT 'warehouse:update', '仓库编辑', 33 UNION ALL
    SELECT 'warehouse:delete', '仓库删除', 34
) actions ON parent.permission_code = 'warehouse';

INSERT IGNORE INTO sys_permission (permission_code, permission_name, parent_id, type, sort_order)
SELECT code, name, parent.id, 2, sort_order
FROM sys_permission parent
JOIN (
    SELECT 'product:view' code, '商品查看' name, 41 sort_order UNION ALL
    SELECT 'product:create', '商品新增', 42 UNION ALL
    SELECT 'product:update', '商品编辑', 43 UNION ALL
    SELECT 'product:delete', '商品删除', 44
) actions ON parent.permission_code = 'product';

INSERT IGNORE INTO sys_permission (permission_code, permission_name, parent_id, type, sort_order)
SELECT code, name, parent.id, 2, sort_order
FROM sys_permission parent
JOIN (
    SELECT 'stock:view' code, '库存查看' name, 51 sort_order UNION ALL
    SELECT 'stock:adjust', '库存调整', 52 UNION ALL
    SELECT 'stock:check', '库存盘点', 53 UNION ALL
    SELECT 'stock:log', '库存流水', 54
) actions ON parent.permission_code = 'stock';

INSERT IGNORE INTO sys_permission (permission_code, permission_name, parent_id, type, sort_order)
SELECT code, name, parent.id, 2, sort_order
FROM sys_permission parent
JOIN (
    SELECT 'order:view' code, '订单查看' name, 61 sort_order UNION ALL
    SELECT 'order:import', '订单导入', 62 UNION ALL
    SELECT 'order:update', '订单编辑', 63
) actions ON parent.permission_code = 'order';

INSERT IGNORE INTO sys_permission (permission_code, permission_name, parent_id, type, sort_order)
SELECT code, name, parent.id, 2, sort_order
FROM sys_permission parent
JOIN (
    SELECT 'outbound:view' code, '发货查看' name, 71 sort_order UNION ALL
    SELECT 'outbound:create', '发货创建编辑', 72 UNION ALL
    SELECT 'outbound:scan', '发货扫码', 73 UNION ALL
    SELECT 'outbound:confirm', '确认发货', 74 UNION ALL
    SELECT 'outbound:cancel', '取消发货', 75
) actions ON parent.permission_code = 'outbound';

INSERT IGNORE INTO sys_permission (permission_code, permission_name, parent_id, type, sort_order)
SELECT code, name, parent.id, 2, sort_order
FROM sys_permission parent
JOIN (
    SELECT 'return:view' code, '退货查看' name, 81 sort_order UNION ALL
    SELECT 'return:create', '退货创建', 82 UNION ALL
    SELECT 'return:check', '退货质检', 83 UNION ALL
    SELECT 'return:confirm', '退货入库', 84 UNION ALL
    SELECT 'return:cancel', '取消退货', 85
) actions ON parent.permission_code = 'return';

INSERT IGNORE INTO sys_permission (permission_code, permission_name, parent_id, type, sort_order)
SELECT code, name, parent.id, 2, sort_order
FROM sys_permission parent
JOIN (
    SELECT 'express:view' code, '快递查询计费' name, 91 sort_order UNION ALL
    SELECT 'express:company', '快递公司管理', 92 UNION ALL
    SELECT 'express:template', '费用模板管理', 93 UNION ALL
    SELECT 'express:report', '快递费用统计', 94
) actions ON parent.permission_code = 'express';

INSERT IGNORE INTO sys_permission (permission_code, permission_name, parent_id, type, sort_order)
SELECT code, name, parent.id, 2, sort_order
FROM sys_permission parent
JOIN (
    SELECT 'system:user' code, '用户管理' name, 101 sort_order UNION ALL
    SELECT 'system:role', '角色权限管理', 102 UNION ALL
    SELECT 'system:file', '文件管理', 103 UNION ALL
    SELECT 'system:log', '操作日志', 104 UNION ALL
    SELECT 'system:stock-alert', '库存预警设置', 105 UNION ALL
    SELECT 'system:stock-alert-template', '预警模板管理', 106 UNION ALL
    SELECT 'system:platform', '平台管理', 107
) actions ON parent.permission_code = 'system';

INSERT IGNORE INTO sys_role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM sys_role r
JOIN sys_permission p
WHERE r.role_code = 'SUPER_ADMIN';

INSERT IGNORE INTO sys_role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM sys_role r
JOIN sys_permission p ON p.permission_code IN (
    'dashboard:view',
    'warehouse:view', 'warehouse:create', 'warehouse:update',
    'product:view',
    'stock:view', 'stock:adjust', 'stock:check', 'stock:log',
    'order:view', 'order:update',
    'outbound:view', 'outbound:create', 'outbound:scan', 'outbound:confirm', 'outbound:cancel',
    'return:view', 'return:create', 'return:check', 'return:confirm', 'return:cancel',
    'express:view', 'express:company', 'express:template', 'express:report'
)
WHERE r.role_code = 'WAREHOUSE_ADMIN';

INSERT IGNORE INTO sys_role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM sys_role r
JOIN sys_permission p ON p.permission_code IN (
    'dashboard:view',
    'stock:view', 'stock:log',
    'outbound:view', 'outbound:scan', 'outbound:confirm',
    'return:view', 'return:check', 'return:confirm'
)
WHERE r.role_code = 'OPERATOR';

INSERT IGNORE INTO sys_role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM sys_role r
JOIN sys_permission p ON p.permission_code IN (
    'dashboard:view',
    'warehouse:view',
    'product:view',
    'stock:view', 'stock:log',
    'order:view',
    'outbound:view',
    'return:view',
    'express:view',
    'express:report'
)
WHERE r.role_code = 'VIEWER';
```

- [ ] **Step 2: Commit**

Run:

```powershell
git add wms-backend/sql/V20260616__custom_role_permissions.sql
git commit -m "feat: seed custom role permissions"
```

---

### Task 8: Frontend Auth Types, Store, Menu, and Router

**Files:**
- Modify: `wsm-web/src/api/auth.ts`
- Modify: `wsm-web/src/api/user.ts`
- Modify: `wsm-web/src/stores/user.ts`
- Modify: `wsm-web/src/composables/usePermission.ts`
- Modify: `wsm-web/src/utils/constants.ts`
- Modify: `wsm-web/src/router/index.ts`
- Modify: `wsm-web/src/layouts/DefaultLayout.vue`

- [ ] **Step 1: Update auth API types**

In `auth.ts`, use string roles and permissions:

```ts
export interface LoginResult {
  token: string
  userId: number
  username: string
  roles: string[]
  permissions: string[]
}

export interface UserProfile {
  id: number
  username: string
  realName: string
  phone: string
  roles: string[]
  permissions: string[]
}
```

- [ ] **Step 2: Add role and permission API types**

In `user.ts`, extend `Role` and add functions:

```ts
export interface Permission {
  id: number
  permissionCode: string
  permissionName: string
  parentId: number
  type: number
  sortOrder: number
  children?: Permission[]
}

export interface Role {
  id: number
  roleCode: string
  roleName: string
  description: string
  permissionIds?: number[]
  permissionCodes?: string[]
}

export interface RoleForm {
  roleCode: string
  roleName: string
  description?: string
  permissionIds?: number[]
}

export function getRoleDetail(id: number) {
  return request.get<any, ApiResponse<Role>>(`/roles/${id}`)
}

export function createRole(data: RoleForm) {
  return request.post<any, ApiResponse<number>>('/roles', data)
}

export function updateRole(id: number, data: RoleForm) {
  return request.put<any, ApiResponse<void>>(`/roles/${id}`, data)
}

export function deleteRole(id: number) {
  return request.delete<any, ApiResponse<void>>(`/roles/${id}`)
}

export function getPermissionTree() {
  return request.get<any, ApiResponse<Permission[]>>('/permissions/tree')
}

export function updateRolePermissions(id: number, permissionIds: number[]) {
  return request.put<any, ApiResponse<void>>(`/roles/${id}/permissions`, { permissionIds })
}
```

- [ ] **Step 3: Update user store**

In `stores/user.ts`, add:

```ts
const permissions = ref<string[]>([])
```

In `login`, after roles assignment:

```ts
permissions.value = res.data.permissions || []
```

In `fetchProfile`, replace the role mapping line with:

```ts
roles.value = res.data.roles || []
permissions.value = res.data.permissions || []
```

In `logout`, add:

```ts
permissions.value = []
```

Return `permissions`:

```ts
return { token, userInfo, roles, permissions, login, fetchProfile, logout }
```

- [ ] **Step 4: Update permission composable**

Replace `usePermission.ts` with:

```ts
import { computed } from 'vue'
import { useUserStore } from '@/stores/user'

export function usePermission() {
  const userStore = useUserStore()

  const isSuperAdmin = computed(() => userStore.roles.includes('SUPER_ADMIN'))
  const isAdmin = computed(() => userStore.roles.includes('ADMIN') || isSuperAdmin.value)

  function hasRole(role: string) {
    return userStore.roles.includes(role)
  }

  function hasAnyRole(roles: string[]) {
    return roles.some((role) => userStore.roles.includes(role))
  }

  function hasPermission(code: string) {
    return isSuperAdmin.value || userStore.permissions.includes(code)
  }

  function hasAnyPermission(codes: string[]) {
    return isSuperAdmin.value || codes.some((code) => userStore.permissions.includes(code))
  }

  function hasAllPermissions(codes: string[]) {
    return isSuperAdmin.value || codes.every((code) => userStore.permissions.includes(code))
  }

  return {
    isSuperAdmin,
    isAdmin,
    hasRole,
    hasAnyRole,
    hasPermission,
    hasAnyPermission,
    hasAllPermissions,
  }
}
```

- [ ] **Step 5: Add menu permissions**

In `constants.ts`, extend `MenuItem`:

```ts
permission?: string
```

Add permissions to menu items:

```ts
{ title: '仪表盘', icon: 'Odometer', path: '/dashboard', permission: 'dashboard:view' }
```

For children, use:

```ts
{ title: 'AI 助手', path: '/ai/assistant', permission: 'ai:assistant' }
{ title: '知识库管理', path: '/ai/knowledge', permission: 'ai:knowledge' }
{ title: '仓库管理', path: '/warehouse/list', permission: 'warehouse:view' }
{ title: '货架管理', path: '/warehouse/shelves', permission: 'warehouse:view' }
{ title: '特殊仓库管理', path: '/warehouse/special', permission: 'stock:view' }
{ title: '商品列表', path: '/product/list', permission: 'product:view' }
{ title: 'SKU 管理', path: '/product/sku', permission: 'product:view' }
{ title: '库存查询', path: '/stock/query', permission: 'stock:view' }
{ title: '库存流水', path: '/stock/log', permission: 'stock:log' }
{ title: '盘点管理', path: '/stock/check', permission: 'stock:check' }
{ title: '订单管理', path: '/order/list', permission: 'order:view' }
{ title: '发货管理', path: '/outbound/list', permission: 'outbound:view' }
{ title: '退货管理', path: '/returns/list', permission: 'return:view' }
{ title: '快递查询', path: '/express/query', permission: 'express:view' }
{ title: '快递公司管理', path: '/express/companies', permission: 'express:company' }
{ title: '费用模板管理', path: '/express/fee-templates', permission: 'express:template' }
{ title: '快递费用统计', path: '/express/fee-report', permission: 'express:report' }
{ title: '用户管理', path: '/system/users', permission: 'system:user' }
{ title: '角色管理', path: '/system/roles', permission: 'system:role' }
{ title: '文件管理', path: '/system/files', permission: 'system:file' }
{ title: '操作日志', path: '/system/logs', permission: 'system:log' }
{ title: '库存预警设置', path: '/system/stock-alert', permission: 'system:stock-alert' }
{ title: '预警模板管理', path: '/system/stock-alert-template', permission: 'system:stock-alert-template' }
{ title: '平台管理', path: '/system/platforms', permission: 'system:platform' }
```

- [ ] **Step 6: Add route permissions**

In `router/index.ts`, add `permission` to each route meta using the same codes as the menu. Add the role route:

```ts
{
  path: 'system/roles',
  name: 'RoleList',
  component: () => import('@/views/system/RoleList.vue'),
  meta: { title: '角色管理', icon: 'Lock', permission: 'system:role' },
}
```

Replace the guard with:

```ts
router.beforeEach(async (to, _from, next) => {
  const token = localStorage.getItem('token')
  if (to.path !== '/login' && !token) {
    next('/login')
    return
  }
  if (to.path === '/login' && token) {
    next('/dashboard')
    return
  }
  if (!token) {
    next()
    return
  }

  const { useUserStore } = await import('@/stores/user')
  const userStore = useUserStore()
  if (!userStore.userInfo || userStore.permissions.length === 0) {
    await userStore.fetchProfile().catch(() => {
      userStore.logout()
      next('/login')
    })
    if (!userStore.token) return
  }

  const requiredPermission = to.meta?.permission as string | undefined
  const allowed = !requiredPermission
    || userStore.roles.includes('SUPER_ADMIN')
    || userStore.permissions.includes(requiredPermission)
  if (!allowed) {
    const firstRoute = findFirstAccessibleRoute(userStore.permissions, userStore.roles)
    next(firstRoute || '/dashboard')
    return
  }
  next()
})
```

Add helper functions below the guard:

```ts
function findFirstAccessibleRoute(permissions: string[], roles: string[]) {
  const superAdmin = roles.includes('SUPER_ADMIN')
  const root = routes.find((route) => route.path === '/')
  const child = root?.children?.find((route) => {
    const permission = route.meta?.permission as string | undefined
    return !permission || superAdmin || permissions.includes(permission)
  })
  return child ? `/${child.path}` : undefined
}
```

- [ ] **Step 7: Filter sidebar menu**

In `DefaultLayout.vue`, replace `MENU_LIST` usage with a computed filtered menu:

```ts
import type { MenuItem } from '@/utils/constants'
import { usePermission } from '@/composables/usePermission'

const { hasPermission } = usePermission()

const visibleMenus = computed(() => filterMenus(MENU_LIST))

function filterMenus(items: MenuItem[]): MenuItem[] {
  return items
    .map((item) => {
      const children = item.children ? filterMenus(item.children) : undefined
      const itemAllowed = !item.permission || hasPermission(item.permission)
      if (children && children.length > 0) {
        return { ...item, children }
      }
      if (!children && itemAllowed) {
        return item
      }
      return null
    })
    .filter((item): item is MenuItem => item !== null)
}
```

In the template, replace `MENU_LIST` with `visibleMenus`.

- [ ] **Step 8: Build frontend**

Run:

```powershell
cd wsm-web
npm run build
```

Expected: PASS. If TypeScript reports route meta typing warnings, add casts like `as string | undefined` where route meta is read.

- [ ] **Step 9: Commit**

Run:

```powershell
git add wsm-web/src/api/auth.ts `
  wsm-web/src/api/user.ts `
  wsm-web/src/stores/user.ts `
  wsm-web/src/composables/usePermission.ts `
  wsm-web/src/utils/constants.ts `
  wsm-web/src/router/index.ts `
  wsm-web/src/layouts/DefaultLayout.vue
git commit -m "feat: add frontend permission filtering"
```

---

### Task 9: Role Management Page

**Files:**
- Create: `wsm-web/src/views/system/RoleList.vue`

- [ ] **Step 1: Create the role page**

Create `RoleList.vue` using Element Plus table, dialog form, and permission tree:

```vue
<template>
  <div class="page-container">
    <PageHeader title="角色管理">
      <template #actions>
        <el-button type="primary" icon="Plus" @click="openDialog()">新增角色</el-button>
      </template>
    </PageHeader>

    <div class="card">
      <el-table :data="roleList" v-loading="loading" stripe border>
        <el-table-column prop="roleCode" label="角色编码" width="180" />
        <el-table-column prop="roleName" label="角色名称" width="160" />
        <el-table-column prop="description" label="描述" min-width="220" show-overflow-tooltip />
        <el-table-column label="权限数量" width="100" align="center">
          <template #default="{ row }">{{ row.permissionCodes?.length || 0 }}</template>
        </el-table-column>
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link icon="Edit" @click="openDialog(row)">编辑</el-button>
            <el-button type="success" link icon="Lock" @click="openPermissionDialog(row)">权限</el-button>
            <el-popconfirm
              v-if="!isBuiltInRole(row.roleCode)"
              title="确定删除该角色吗？"
              @confirm="handleDelete(row.id)"
            >
              <template #reference>
                <el-button type="danger" link icon="Delete">删除</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑角色' : '新增角色'" width="520px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="角色编码" prop="roleCode">
          <el-input v-model="form.roleCode" :disabled="isEdit" />
        </el-form-item>
        <el-form-item label="角色名称" prop="roleName">
          <el-input v-model="form.roleName" />
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input v-model="form.description" type="textarea" :rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="permissionVisible" title="权限配置" width="640px" destroy-on-close>
      <div class="permission-header">
        <span>{{ currentRole?.roleName }}</span>
        <el-tag size="small">{{ currentRole?.roleCode }}</el-tag>
      </div>
      <el-tree
        ref="treeRef"
        :data="permissionTree"
        show-checkbox
        node-key="id"
        default-expand-all
        :props="{ label: 'permissionName', children: 'children' }"
      />
      <template #footer>
        <el-button @click="permissionVisible = false">取消</el-button>
        <el-button type="primary" :loading="savingPermissions" @click="handleSavePermissions">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { nextTick, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules, TreeInstance } from 'element-plus'
import PageHeader from '@/components/PageHeader.vue'
import {
  createRole,
  deleteRole,
  getPermissionTree,
  getRoleList,
  updateRole,
  updateRolePermissions,
} from '@/api/user'
import type { Permission, Role, RoleForm } from '@/api/user'

const BUILT_IN_ROLES = ['SUPER_ADMIN', 'WAREHOUSE_ADMIN', 'OPERATOR', 'VIEWER']

const loading = ref(false)
const roleList = ref<Role[]>([])
const permissionTree = ref<Permission[]>([])
const dialogVisible = ref(false)
const permissionVisible = ref(false)
const isEdit = ref(false)
const submitting = ref(false)
const savingPermissions = ref(false)
const currentRole = ref<Role | null>(null)
const formRef = ref<FormInstance>()
const treeRef = ref<TreeInstance>()

const form = reactive<RoleForm>({
  roleCode: '',
  roleName: '',
  description: '',
  permissionIds: [],
})

const rules: FormRules = {
  roleCode: [{ required: true, message: '请输入角色编码', trigger: 'blur' }],
  roleName: [{ required: true, message: '请输入角色名称', trigger: 'blur' }],
}

onMounted(async () => {
  await Promise.all([fetchRoles(), fetchPermissionTree()])
})

function isBuiltInRole(roleCode: string) {
  return BUILT_IN_ROLES.includes(roleCode)
}

async function fetchRoles() {
  loading.value = true
  try {
    const res = await getRoleList()
    roleList.value = res.data || []
  } finally {
    loading.value = false
  }
}

async function fetchPermissionTree() {
  const res = await getPermissionTree()
  permissionTree.value = res.data || []
}

function openDialog(row?: Role) {
  isEdit.value = !!row
  Object.assign(form, {
    roleCode: row?.roleCode || '',
    roleName: row?.roleName || '',
    description: row?.description || '',
    permissionIds: row?.permissionIds || [],
  })
  currentRole.value = row || null
  dialogVisible.value = true
}

async function handleSubmit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    if (isEdit.value && currentRole.value) {
      await updateRole(currentRole.value.id, form)
      ElMessage.success('修改成功')
    } else {
      await createRole(form)
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    await fetchRoles()
  } finally {
    submitting.value = false
  }
}

async function openPermissionDialog(row: Role) {
  currentRole.value = row
  permissionVisible.value = true
  await nextTick()
  treeRef.value?.setCheckedKeys(row.permissionIds || [])
}

async function handleSavePermissions() {
  if (!currentRole.value) return
  savingPermissions.value = true
  try {
    const checked = treeRef.value?.getCheckedKeys(false) || []
    const halfChecked = treeRef.value?.getHalfCheckedKeys() || []
    const permissionIds = [...checked, ...halfChecked].map((id) => Number(id))
    await updateRolePermissions(currentRole.value.id, permissionIds)
    ElMessage.success('权限已保存')
    permissionVisible.value = false
    await fetchRoles()
  } finally {
    savingPermissions.value = false
  }
}

async function handleDelete(id: number) {
  await deleteRole(id)
  ElMessage.success('删除成功')
  await fetchRoles()
}
</script>

<style scoped lang="scss">
.permission-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 16px;
  font-weight: 600;
  color: #334155;
}
</style>
```

- [ ] **Step 2: Build frontend**

Run:

```powershell
cd wsm-web
npm run build
```

Expected: PASS.

- [ ] **Step 3: Commit**

Run:

```powershell
git add wsm-web/src/views/system/RoleList.vue
git commit -m "feat: add role management page"
```

---

### Task 10: Frontend Operation Button Guards

**Files:**
- Modify: `wsm-web/src/views/system/UserList.vue`
- Modify: `wsm-web/src/views/product/ProductList.vue`
- Modify: `wsm-web/src/views/product/SkuList.vue`
- Modify: `wsm-web/src/views/warehouse/WarehouseList.vue`
- Modify: `wsm-web/src/views/warehouse/ShelfList.vue`
- Modify: `wsm-web/src/views/warehouse/SpecialWarehouseView.vue`
- Modify: `wsm-web/src/views/stock/StockQuery.vue`
- Modify: `wsm-web/src/views/stock/StockCheck.vue`
- Modify: `wsm-web/src/views/order/OrderList.vue`
- Modify: `wsm-web/src/views/outbound/OutboundList.vue`
- Modify: `wsm-web/src/views/returns/ReturnList.vue`
- Modify: `wsm-web/src/views/express/ExpressQuery.vue`
- Modify: `wsm-web/src/views/express/ExpressCompanyList.vue`
- Modify: `wsm-web/src/views/express/ExpressFeeTemplate.vue`
- Modify: `wsm-web/src/views/system/FileUpload.vue`
- Modify: `wsm-web/src/views/system/StockAlertConfig.vue`
- Modify: `wsm-web/src/views/system/StockAlertTemplate.vue`
- Modify: `wsm-web/src/views/system/PlatformList.vue`

- [ ] **Step 1: Guard user management actions**

In `UserList.vue`, import permission helper:

```ts
import { usePermission } from '@/composables/usePermission'

const { hasPermission } = usePermission()
```

Wrap actions:

```vue
<el-button v-if="hasPermission('system:user')" type="primary" icon="Plus" @click="openDialog()">新增用户</el-button>
```

For row operations, keep buttons visible only when `hasPermission('system:user')` is true.

- [ ] **Step 2: Guard product actions in exact files**

In `wsm-web/src/views/product/ProductList.vue` and `wsm-web/src/views/product/SkuList.vue`, use:

```ts
const { hasPermission } = usePermission()
```

Use these checks:

```text
Create/import buttons: product:create
Edit buttons: product:update
Delete buttons: product:delete
Read-only detail buttons: product:view
```

- [ ] **Step 3: Guard warehouse actions in exact files**

In `wsm-web/src/views/warehouse/WarehouseList.vue`, `wsm-web/src/views/warehouse/ShelfList.vue`, and `wsm-web/src/views/warehouse/SpecialWarehouseView.vue`, use:

```text
Create buttons: warehouse:create
Edit buttons: warehouse:update
Delete buttons: warehouse:delete
Read-only navigation/detail buttons: warehouse:view
```

- [ ] **Step 4: Guard stock actions in exact files**

In `wsm-web/src/views/stock/StockQuery.vue` and `wsm-web/src/views/stock/StockCheck.vue`, use:

```text
Inventory adjustment and special-stock confirm buttons: stock:adjust
Stock check create/submit buttons: stock:check
Stock log page actions: stock:log
```

- [ ] **Step 5: Guard order/outbound/return actions in exact files**

In `wsm-web/src/views/order/OrderList.vue`, `wsm-web/src/views/outbound/OutboundList.vue`, and `wsm-web/src/views/returns/ReturnList.vue`, use:

Use:

```text
Order import: order:import
Order edit/status update: order:update
Outbound create/update: outbound:create
Outbound scan: outbound:scan
Outbound confirm: outbound:confirm
Outbound cancel: outbound:cancel
Return create: return:create
Return quality check: return:check
Return confirm: return:confirm
Return cancel: return:cancel
```

- [ ] **Step 6: Guard express and system actions in exact files**

In `wsm-web/src/views/express/ExpressQuery.vue`, `wsm-web/src/views/express/ExpressCompanyList.vue`, `wsm-web/src/views/express/ExpressFeeTemplate.vue`, `wsm-web/src/views/system/FileUpload.vue`, `wsm-web/src/views/system/StockAlertConfig.vue`, `wsm-web/src/views/system/StockAlertTemplate.vue`, and `wsm-web/src/views/system/PlatformList.vue`, use:

Use:

```text
Express query/calculate: express:view
Express company create/edit/delete: express:company
Express fee template create/edit/delete: express:template
Stock alert config actions: system:stock-alert
Stock alert template actions: system:stock-alert-template
Platform create/edit/delete: system:platform
File upload/delete: system:file
```

- [ ] **Step 7: Build frontend**

Run:

```powershell
cd wsm-web
npm run build
```

Expected: PASS.

- [ ] **Step 8: Commit**

Run:

```powershell
git add wsm-web/src/views
git commit -m "feat: hide unauthorized operation buttons"
```

---

### Task 11: Full Verification

**Files:**
- No new files required.

- [ ] **Step 1: Run focused backend tests**

Run:

```powershell
cd wms-backend
.\mvnw.cmd -Dtest=PermissionServiceImplTest,RoleServiceImplTest,PermissionInterceptorTest,JwtAuthFilterTest test
```

Expected: PASS.

- [ ] **Step 2: Run full backend tests**

Run:

```powershell
cd wms-backend
.\mvnw.cmd test
```

Expected: PASS.

- [ ] **Step 3: Run frontend build**

Run:

```powershell
cd wsm-web
npm run build
```

Expected: PASS.

- [ ] **Step 4: Manual browser verification**

Start backend if needed:

```powershell
cd wms-backend
.\mvnw.cmd spring-boot:run
```

Start frontend if needed:

```powershell
cd wsm-web
npm run dev
```

Verify:

```text
1. Login as admin / 123456.
2. Open 系统管理 -> 角色管理.
3. Create a custom role named CUSTOMER_SERVICE.
4. Grant dashboard:view and order:view only.
5. Assign the role to a test user.
6. Login as that test user.
7. Confirm only dashboard and order menu entries are visible.
8. Directly open /system/users and confirm the route is blocked.
9. Call POST /api/products with the test user's token and confirm 403.
10. Grant product:create to the role, refresh, call POST /api/products again with valid payload, and confirm the permission is no longer the blocking error.
```

- [ ] **Step 5: Final commit for verification-only fixes**

If verification required small fixes, commit them:

```powershell
git add wms-backend wsm-web
git commit -m "fix: polish custom role permissions"
```

If verification required no fixes, do not create an empty commit.

---

## Self-Review Notes

Spec coverage:

- Custom roles: Task 4 and Task 6.
- Role as template: Task 4, Task 6, Task 9.
- Module/action permissions: Task 7 catalog and Task 8 frontend metadata.
- Account role assignment compatibility: Task 6 keeps existing user role assignment and Task 8 keeps `getRoleList`.
- Backend no-overreach guard: Task 5.
- Frontend menus/routes/buttons: Task 8, Task 9, Task 10.
- Default roles remain compatible: Task 7.
- Super-admin all-permission rule: Task 3.
- Safety rules: Task 4 and Task 6.

Type consistency:

- Backend effective permissions are `List<String>` in `LoginResponse` and `UserVO`.
- Frontend effective permissions are `string[]` in login/profile/store.
- Role permission IDs are `List<Long>` in backend and `number[]` in frontend.
- Permission tree children use `children` in both backend and frontend.
