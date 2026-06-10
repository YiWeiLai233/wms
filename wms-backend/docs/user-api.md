# 用户管理 API 接口文档

> 基础路径：`http://localhost:8080/api`
>
> 认证方式：JWT Token（Header: `Authorization: Bearer {token}`）

---

## 1. 用户登录

**POST** `/api/auth/login`

**请求参数：**

```json
{
  "username": "admin",
  "password": "123456"
}
```

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| username | String | 是 | 用户名 |
| password | String | 是 | 密码 |

**响应示例：**

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "userId": 1,
    "username": "admin",
    "realName": "超级管理员",
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "roles": ["SUPER_ADMIN"]
  }
}
```

---

## 2. 获取当前用户信息

**GET** `/api/auth/profile`

**请求头：**

```
Authorization: Bearer {token}
```

**响应示例：**

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 1,
    "username": "admin",
    "realName": "超级管理员",
    "phone": "13800138000",
    "email": "admin@example.com",
    "status": 1,
    "roles": ["SUPER_ADMIN"],
    "roleIds": [1],
    "createdAt": "2026-06-08T10:00:00"
  }
}
```

---

## 3. 用户列表

**GET** `/api/users`

**请求参数：**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| keyword | String | 否 | 关键词（用户名/姓名/手机号） |
| pageNum | Integer | 否 | 页码，默认1 |
| pageSize | Integer | 否 | 每页数量，默认20 |

**响应示例：**

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "total": 10,
    "list": [
      {
        "id": 1,
        "username": "admin",
        "realName": "超级管理员",
        "phone": "13800138000",
        "email": "admin@example.com",
        "status": 1,
        "roles": ["SUPER_ADMIN"],
        "createdAt": "2026-06-08T10:00:00"
      },
      {
        "id": 2,
        "username": "zhangsan",
        "realName": "张三",
        "phone": "13900139000",
        "email": "zhangsan@example.com",
        "status": 1,
        "roles": ["OPERATOR"],
        "createdAt": "2026-06-09T10:00:00"
      }
    ]
  }
}
```

---

## 4. 用户详情

**GET** `/api/users/{id}`

**路径参数：**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| id | Long | 是 | 用户ID |

**响应示例：**

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 2,
    "username": "zhangsan",
    "realName": "张三",
    "phone": "13900139000",
    "email": "zhangsan@example.com",
    "status": 1,
    "roles": ["OPERATOR", "WAREHOUSE_ADMIN"],
    "roleIds": [3, 2],
    "createdAt": "2026-06-09T10:00:00"
  }
}
```

---

## 5. 新增用户

**POST** `/api/users`

**请求参数：**

```json
{
  "username": "zhangsan",
  "password": "123456",
  "realName": "张三",
  "phone": "13900139000",
  "email": "zhangsan@example.com",
  "status": 1,
  "roleIds": [2, 3]
}
```

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| username | String | 是 | 用户名（唯一） |
| password | String | 是 | 密码 |
| realName | String | 否 | 真实姓名 |
| phone | String | 否 | 手机号 |
| email | String | 否 | 邮箱 |
| status | Integer | 否 | 状态：0-禁用 1-启用，默认1 |
| roleIds | Array | 否 | 角色ID列表 |

**响应示例：**

```json
{
  "code": 200,
  "message": "success",
  "data": 2
}
```

**错误码：**

| 错误码 | 说明 |
|--------|------|
| 1004 | 用户已存在 |

---

## 6. 修改用户

**PUT** `/api/users`

**请求参数：**

```json
{
  "id": 2,
  "realName": "张三-修改",
  "phone": "13900139001",
  "email": "zhangsan_new@example.com",
  "status": 1,
  "roleIds": [2]
}
```

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| id | Long | 是 | 用户ID |
| realName | String | 否 | 真实姓名 |
| phone | String | 否 | 手机号 |
| email | String | 否 | 邮箱 |
| status | Integer | 否 | 状态：0-禁用 1-启用 |
| roleIds | Array | 否 | 角色ID列表（传入会覆盖原有角色） |

**响应示例：**

```json
{
  "code": 200,
  "message": "success",
  "data": null
}
```

**错误码：**

| 错误码 | 说明 |
|--------|------|
| 1001 | 用户不存在 |

---

## 7. 删除用户

**DELETE** `/api/users/{id}`

**路径参数：**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| id | Long | 是 | 用户ID |

**响应示例：**

```json
{
  "code": 200,
  "message": "success",
  "data": null
}
```

**错误码：**

| 错误码 | 说明 |
|--------|------|
| 1001 | 用户不存在 |

**说明：** 逻辑删除，不会真正删除数据

---

## 8. 重置密码

**PUT** `/api/users/{id}/reset-password`

**路径参数：**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| id | Long | 是 | 用户ID |

**请求参数（Query）：**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| newPassword | String | 是 | 新密码 |

**请求示例：**

```
PUT /api/users/2/reset-password?newPassword=654321
```

**响应示例：**

```json
{
  "code": 200,
  "message": "success",
  "data": null
}
```

**错误码：**

| 错误码 | 说明 |
|--------|------|
| 1001 | 用户不存在 |

---

## 9. 角色列表

**GET** `/api/roles`

**响应示例：**

```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "id": 1,
      "roleCode": "SUPER_ADMIN",
      "roleName": "超级管理员",
      "description": "全部权限"
    },
    {
      "id": 2,
      "roleCode": "WAREHOUSE_ADMIN",
      "roleName": "仓库管理员",
      "description": "入库、出库、退货、库存盘点"
    },
    {
      "id": 3,
      "roleCode": "OPERATOR",
      "roleName": "仓库操作员",
      "description": "扫码入库、扫码出库、库存查询"
    },
    {
      "id": 4,
      "roleCode": "VIEWER",
      "roleName": "查询员",
      "description": "只读查询"
    }
  ]
}
```

---

## 角色说明

| 角色编码 | 角色名称 | 权限说明 |
|----------|----------|----------|
| SUPER_ADMIN | 超级管理员 | 全部权限 |
| WAREHOUSE_ADMIN | 仓库管理员 | 入库、出库、退货、库存盘点 |
| OPERATOR | 仓库操作员 | 扫码入库、扫码出库、库存查询 |
| VIEWER | 查询员 | 只读查询 |

---

## 用户状态

| 值 | 说明 |
|----|------|
| 0 | 禁用 |
| 1 | 启用 |

---

## 错误码

| 错误码 | 说明 |
|--------|------|
| 200 | 操作成功 |
| 400 | 请求参数错误 |
| 401 | 未登录或登录已过期 |
| 403 | 无权限访问 |
| 1001 | 用户不存在 |
| 1002 | 密码错误 |
| 1003 | 用户已禁用 |
| 1004 | 用户已存在 |

---

## 前端使用示例

### Vue/React 示例

```javascript
// 用户登录
const login = async (username, password) => {
  const res = await axios.post('/api/auth/login', { username, password });
  const { token, userId, roles } = res.data.data;
  // 保存 token
  localStorage.setItem('token', token);
  // 设置请求头
  axios.defaults.headers.common['Authorization'] = `Bearer ${token}`;
};

// 获取用户列表
const getUsers = async (keyword, pageNum, pageSize) => {
  const res = await axios.get('/api/users', {
    params: { keyword, pageNum, pageSize }
  });
  return res.data.data;
};

// 新增用户
const createUser = async (userData) => {
  const res = await axios.post('/api/users', userData);
  return res.data.data; // 返回用户ID
};

// 修改用户
const updateUser = async (userData) => {
  await axios.put('/api/users', userData);
};

// 删除用户
const deleteUser = async (id) => {
  await axios.delete(`/api/users/${id}`);
};

// 重置密码
const resetPassword = async (id, newPassword) => {
  await axios.put(`/api/users/${id}/reset-password`, null, {
    params: { newPassword }
  });
};

// 获取角色列表
const getRoles = async () => {
  const res = await axios.get('/api/roles');
  return res.data.data;
};
```

---

*文档生成时间：2026-06-09*
