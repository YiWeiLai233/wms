# 易仓 WMS 后端框架设计

> **职责声明：本文档仅涉及后端部分。** 前端（Electron 桌面端、Android 扫码端）由其他成员负责，本文档不包含前端相关内容。

---

## 1. 项目定位

```text
项目名称：易仓 WMS
系统定位：局域网多端仓库管理系统（后端部分）
```

后端职责：

- 提供统一的 RESTful API 接口
- 处理所有业务逻辑
- 管理数据持久化（MySQL）
- 订单快速查询索引（Elasticsearch）
- 文件上传与存储
- 用户认证与权限控制

---

## 2. 实际技术栈

> 以下基于项目 `pom.xml` 中已引入的依赖，非理想化选型。

| 组件 | 技术 | 版本 |
|------|------|------|
| 框架 | Spring Boot | 4.0.6 |
| ORM | MyBatis | mybatis-spring-boot-starter 4.0.1 |
| 数据库 | MySQL | mysql-connector-j |
| JDK | Java | 17 |
| 工具库 | Lombok | 随 Spring Boot 管理 |
| 构建 | Maven | mvnw |

### 待引入依赖（按开发顺序）

| 组件 | 用途 | 优先级 |
|------|------|--------|
| Spring Security + JWT | 用户认证 | 高 |
| Elasticsearch Java Client | 订单搜索 | 中 |
| Knife4j / SpringDoc | API 文档 | 中 |
| PageHelper 或 MyBatis 分页插件 | 列表分页 | 中 |
| Redis | Token 存储 / 缓存（可选） | 低 |
| XXL-JOB 或 Spring Task | 定时任务（ES 同步） | 低 |

---

## 3. 后端分层架构

```text
com.yiweilai.wms
 ├── common              通用工具、响应封装、常量
 ├── config              配置类（跨域、MyBatis、安全等）
 ├── exception           全局异常处理
 ├── security            认证授权（JWT、过滤器）
 │
 ├── user                用户权限模块
 │   ├── controller
 │   ├── service
 │   ├── mapper
 │   ├── entity
 │   ├── dto
 │   └── vo
 │
 ├── product             商品管理模块
 │   ├── controller
 │   ├── service
 │   ├── mapper
 │   ├── entity
 │   ├── dto
 │   └── vo
 │
 ├── warehouse           仓库库位模块
 │   ├── controller
 │   ├── service
 │   ├── mapper
 │   ├── entity
 │   ├── dto
 │   └── vo
 │
 ├── stock               库存管理模块
 │   ├── controller
 │   ├── service
 │   ├── mapper
 │   ├── entity
 │   ├── dto
 │   └── vo
 │
 ├── order               订单管理模块
 │   ├── controller
 │   ├── service
 │   ├── mapper
 │   ├── entity
 │   ├── dto
 │   └── vo
 │
 ├── outbound            出库管理模块
 │   ├── controller
 │   ├── service
 │   ├── mapper
 │   ├── entity
 │   ├── dto
 │   └── vo
 │
 ├── returns             退货处理模块
 │   ├── controller
 │   ├── service
 │   ├── mapper
 │   ├── entity
 │   ├── dto
 │   └── vo
 │
 ├── search              ES 搜索模块
 │   ├── controller
 │   ├── service
 │   └── document
 │
 ├── file                文件上传模块
 │   ├── controller
 │   └── service
 │
 └── log                 操作日志模块
     ├── controller
     ├── service
     └── mapper
```

---

## 4. 核心模块设计

### 4.1 用户权限模块

**功能：**

- 用户登录 / 登出
- JWT Token 签发与验证
- 用户管理（CRUD）
- 角色管理
- 权限控制

**角色定义：**

```text
SUPER_ADMIN       超级管理员   — 全部权限
WAREHOUSE_ADMIN   仓库管理员   — 入库、出库、退货、库存盘点
OPERATOR          仓库操作员   — 扫码入库、扫码出库、库存查询
VIEWER            查询员      — 只读查询
```

---

### 4.2 商品管理模块

**功能：**

- 商品（SPU）CRUD
- 商品 SKU 管理
- 条码管理
- 商品分类
- 商品图片

**核心实体关系：**

```text
product（商品 SPU）
 ├── product_sku（SKU）
 │    └── product_barcode（条码）
 └── product_category（分类）
```

---

### 4.3 仓库库位模块

**功能：**

- 仓库管理
- 库区管理
- 货架管理
- 库位管理

**层级结构：**

```text
warehouse（仓库）
 └── warehouse_area（库区）
      └── warehouse_shelf（货架）
           └── warehouse_location（库位）

示例：主仓库 / A区 / A01货架 / A01-01库位
```

---

### 4.4 库存管理模块

**功能：**

- 当前库存查询
- 库存调整
- 库存盘点
- 库存流水记录

**库存类型：**

```text
AVAILABLE     可用库存
LOCKED        锁定库存
DEFECTIVE     次品库存
```

**库存流水记录字段：**

```text
业务类型（入库/出库/退货/盘点/锁定/释放）
业务单号
SKU
库位
变动前数量
变动数量
变动后数量
操作人
操作时间
备注
```

---

### 4.5 订单管理模块

**功能：**

- 订单导入
- 订单查询（MySQL）
- 订单快速搜索（ES）
- 订单状态管理

**订单状态枚举：**

```text
WAIT_PAY        待付款
WAIT_OUTBOUND   待出库
OUTBOUNDING     出库中
SHIPPED         已发货
FINISHED        已完成
CANCELLED       已取消
RETURNING       退货中
RETURNED        已退货
```

---

### 4.6 出库管理模块

**功能：**

- 创建出库单
- 扫码核对商品
- 确认出库
- 扣减库存 + 写流水

**出库流程：**

```text
订单进入待出库
  ↓
生成出库单（状态：WAIT_PICKING）
  ↓
仓库拣货（状态：PICKING）
  ↓
扫码核对商品
  ↓
确认出库（事务内执行）
  ├── 扣减库存（防负数 SQL）
  ├── 写库存流水
  ├── 出库单状态 → SHIPPED
  └── 订单状态 → SHIPPED
```

**关键安全要求：**

```text
1. 扣库存必须在事务内执行
2. 防止重复出库（校验出库单状态）
3. 防止库存扣成负数（WHERE quantity >= #{num}）
4. 必须写库存流水
```

---

### 4.7 退货处理模块

**功能：**

- 创建退货单
- 扫描订单 / 商品
- 退货质检
- 可售退回正常库存 / 不可售入次品库存
- 写退货流水

**退货质检状态：**

```text
PENDING_CHECK    待质检
SELLABLE         可售
DEFECTIVE        次品
SCRAPPED         报废
```

---

### 4.8 文件模块

**功能：**

- 商品图片上传
- 退货图片上传
- 出库单附件
- 本地文件存储

**存储策略：**

```text
存储路径：/app/uploads
数据库只存相对路径：/uploads/product/xxx.jpg
后期可替换为 MinIO
```

---

## 5. 核心数据库表设计

### 5.1 用户权限

```sql
sys_user              -- 用户表
sys_role              -- 角色表
sys_permission        -- 权限表
sys_user_role         -- 用户角色关联表
sys_role_permission   -- 角色权限关联表
```

### 5.2 商品

```sql
product               -- 商品表（SPU）
product_sku           -- SKU 表
product_barcode       -- 条码表
product_category      -- 分类表
```

### 5.3 仓库

```sql
warehouse             -- 仓库表
warehouse_area        -- 库区表
warehouse_shelf       -- 货架表
warehouse_location    -- 库位表
```

### 5.4 库存

```sql
stock                 -- 当前库存表
stock_log             -- 库存流水表
stock_check           -- 盘点单表
stock_check_item      -- 盘点明细表
```

### 5.5 订单

```sql
sales_order           -- 订单表
sales_order_item      -- 订单明细表
```

### 5.6 出库

```sql
outbound_order        -- 出库单表
outbound_order_item   -- 出库明细表
```

### 5.7 退货

```sql
return_order          -- 退货单表
return_order_item     -- 退货明细表
```

### 5.8 系统

```sql
file_record           -- 文件记录表
operation_log         -- 操作日志表
es_sync_task          -- ES 同步任务表
```

---

## 6. API 接口设计

### 6.1 认证

```text
POST /api/auth/login          登录
POST /api/auth/logout         登出
GET  /api/auth/profile        获取当前用户信息
```

### 6.2 商品

```text
GET    /api/products           商品列表
POST   /api/products           新增商品
PUT    /api/products/{id}      修改商品
DELETE /api/products/{id}      删除商品

GET    /api/skus               SKU 列表
POST   /api/skus               新增 SKU
PUT    /api/skus/{id}          修改 SKU
DELETE /api/skus/{id}          删除 SKU
```

### 6.3 仓库

```text
GET    /api/warehouses         仓库列表
POST   /api/warehouses         新增仓库
PUT    /api/warehouses/{id}    修改仓库
DELETE /api/warehouses/{id}    删除仓库

GET    /api/locations          库位列表
POST   /api/locations          新增库位
PUT    /api/locations/{id}     修改库位
DELETE /api/locations/{id}     删除库位
```

### 6.4 库存

```text
GET  /api/stocks/query         库存查询
POST /api/stocks/adjust        库存调整
GET  /api/stock-logs           库存流水
POST /api/stocks/check         库存盘点
```

### 6.5 订单

```text
GET  /api/orders               订单列表（MySQL）
GET  /api/orders/{id}          订单详情
GET  /api/orders/search        订单快速搜索（ES）
POST /api/orders/import        订单导入
PUT  /api/orders/{id}/status   修改订单状态
```

### 6.6 出库

```text
POST /api/outbound/create      创建出库单
POST /api/outbound/scan        扫码核对
POST /api/outbound/confirm     确认出库
GET  /api/outbound/{id}        出库单详情
GET  /api/outbound/list        出库单列表
```

### 6.7 退货

```text
POST /api/returns/create       创建退货单
POST /api/returns/scan         扫描订单/商品
POST /api/returns/check        退货质检
POST /api/returns/confirm      确认退货
GET  /api/returns/{id}         退货单详情
GET  /api/returns/list         退货单列表
```

### 6.8 文件

```text
POST   /api/files/upload       上传文件
GET    /api/files/{id}         获取文件
DELETE /api/files/{id}         删除文件
```

---

## 7. Elasticsearch 设计

### 7.1 ES 定位

```text
MySQL        = 主库，负责真实业务数据和事务
Elasticsearch = 查询索引库，只负责订单快速搜索
```

**原则：** 不允许客户端直连 ES 做业务操作，所有写操作必须经过 Spring Boot → MySQL → 同步 ES。

### 7.2 同步方案

```text
MySQL + es_sync_task 表 + 定时任务同步 ES
```

**流程：**

```text
订单新增/修改/出库/退货
  ↓
写 MySQL（事务）
  ↓
写 es_sync_task 同步任务
  ↓
后台定时任务同步 ES
  ↓
同步成功后标记任务完成
```

### 7.3 ES 索引设计

**索引名：** `order_search`

**文档结构：**

```json
{
  "id": 10001,
  "orderNo": "SO202606080001",
  "platformOrderNo": "TB202606080001",
  "receiverName": "张三",
  "receiverPhone": "13800138000",
  "address": "广东省广州市...",
  "orderStatus": "WAIT_OUTBOUND",
  "outboundStatus": "WAIT_PICKING",
  "returnStatus": "NONE",
  "warehouseId": 1,
  "warehouseName": "主仓库",
  "skuCodes": ["SKU001", "SKU002"],
  "barcodes": ["6930000000011"],
  "productNames": ["红色花朵鞋", "黑色鞋带"],
  "totalAmount": 299.00,
  "createdAt": "2026-06-08T18:00:00",
  "searchText": "SO202606080001 TB202606080001 张三 13800138000 红色花朵鞋"
}
```

**字段类型：**

```text
精确查询（keyword）：orderNo, platformOrderNo, receiverPhone, skuCodes, barcodes, orderStatus
模糊查询（text）：receiverName, productNames, address, searchText
范围查询（date/integer）：createdAt, warehouseId
```

### 7.4 同步任务表

```sql
es_sync_task (
  id, biz_type, biz_id, operation,
  status, retry_count, error_msg,
  created_at, updated_at
)
```

**状态：** PENDING → SYNCING → SUCCESS / FAILED

---

## 8. 推荐开发顺序

> 后端开发优先于前端，前端依赖后端 API。

```text
第 1 步：数据库表设计（建表 SQL）
第 2 步：搭建项目基础结构（common、config、exception）
第 3 步：用户登录 + JWT 认证
第 4 步：商品 / SKU / 条码 管理
第 5 步：仓库 / 库区 / 货架 / 库位 管理
第 6 步：库存管理 + 库存流水
第 7 步：订单管理（MySQL 查询）
第 8 步：出库流程（含事务 + 库存扣减）
第 9 步：退货流程
第 10 步：接入 Elasticsearch 订单搜索
第 11 步：文件上传模块
第 12 步：操作日志
第 13 步：报表统计接口
```

---

## 9. 关键安全要求

### 9.1 库存扣减事务

```text
查询库存 → 判断是否足够 → 扣减库存 → 写流水 → 更新出库单状态 → 更新订单状态
以上步骤必须在同一个数据库事务中执行。
```

### 9.2 防止库存为负数

```sql
UPDATE stock
SET quantity = quantity - #{num}
WHERE sku_id = #{skuId}
  AND location_id = #{locationId}
  AND quantity >= #{num};
-- 影响行数为 0 时，说明库存不足
```

### 9.3 防止重复出库

```text
只有 WAIT_PICKING / PICKING 状态的出库单才能确认出库
已 SHIPPED 的出库单不能重复操作
```

### 9.4 库存流水不可省略

```text
任何库存变动都必须写 stock_log，包括：
入库、出库、退货入库、盘点调整、次品入库、锁定、释放
```

---

## 10. 项目结构概览

```text
易仓 WMS 后端
├── Spring Boot 4.0.6 单体架构
├── MyBatis ORM
├── MySQL 主数据库
├── Elasticsearch 订单搜索（后期接入）
├── 本地文件存储
└── Docker Compose 部署

不属于后端职责：
├── Electron 桌面端（前端负责）
├── Android 扫码端（前端负责）
└── UI/UX 设计（前端负责）
```
