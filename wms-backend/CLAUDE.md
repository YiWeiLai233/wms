# CLAUDE.md — 易仓 WMS 后端项目

> **职责声明：本项目仅包含后端代码。** 前端（Electron 桌面端、Android 扫码端）由其他成员负责。

---

## 项目简介

局域网多端仓库管理系统后端，提供 RESTful API，管理商品、仓库、库存、订单、出库、退货等核心业务。

---

## 技术栈

| 组件 | 技术 | 版本 |
|------|------|------|
| 框架 | Spring Boot | 4.0.6 |
| ORM | MyBatis | mybatis-spring-boot-starter 4.0.1 |
| 数据库 | MySQL | mysql-connector-j |
| JDK | Java | 17 |
| 工具库 | Lombok | 随 Spring Boot 管理 |
| 构建 | Maven | mvnw |

---

## 项目结构

```text
com.yiweilai.wms
 ├── common              通用工具、响应封装、常量
 ├── config              配置类（跨域、MyBatis、安全等）
 ├── exception           全局异常处理
 ├── security            认证授权（JWT、过滤器）
 ├── user                用户权限模块
 ├── product             商品管理模块
 ├── warehouse           仓库库位模块
 ├── stock               库存管理模块
 ├── order               订单管理模块
 ├── outbound            出库管理模块
 ├── returns             退货处理模块
 ├── search              ES 搜索模块
 ├── file                文件上传模块
 └── log                 操作日志模块
```

每个模块内部分层：

```text
controller    接口层
service       业务层
mapper        数据库访问层
entity        数据实体
dto           请求参数对象
vo            返回展示对象
enums         枚举
```

---

## 开发规范

### 代码风格

- 使用 Lombok 注解减少样板代码（@Data, @Builder, @Slf4j 等）
- 统一响应封装：`Result<T>` 包含 code、message、data
- 全局异常处理统一返回格式
- SQL 写在 Mapper XML 中，复杂 SQL 需要添加注释

### 命名规范

- 包名：全小写，如 `com.yiweilai.wms.order`
- 类名：大驼峰，如 `SalesOrderService`
- 方法名：小驼峰，如 `getOrderById`
- 数据库表名：下划线分隔，如 `sales_order`
- 数据库字段：下划线分隔，如 `order_no`
- 常量：全大写下划线，如 `ORDER_STATUS_WAIT_OUTBOUND`

### API 规范

- RESTful 风格
- 统一前缀：`/api`
- 返回 JSON
- 分页参数：`page`（从1开始）、`size`
- 分页响应：`total` + `list`

---

## 核心业务规则

### 库存扣减（最高优先级）

```text
1. 必须在数据库事务内执行
2. 扣减 SQL 必须加 WHERE quantity >= #{num} 防止负数
3. 影响行数为 0 时，抛出库存不足异常
4. 必须同步写入 stock_log 库存流水
```

### 防止重复出库

```text
只有 WAIT_PICKING / PICKING 状态的出库单才能确认出库
已 SHIPPED 的出库单不能重复操作
```

### 库存流水不可省略

```text
任何库存变动都必须写 stock_log，包括：
入库、出库、退货入库、盘点调整、次品入库、锁定、释放
```

---

## 关键枚举值

### 订单状态

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

### 退货质检状态

```text
PENDING_CHECK    待质检
SELLABLE         可售
DEFECTIVE        次品
SCRAPPED         报废
```

### 角色

```text
SUPER_ADMIN       超级管理员
WAREHOUSE_ADMIN   仓库管理员
OPERATOR          仓库操作员
VIEWER            查询员
```

---

## 数据库表清单

```text
sys_user / sys_role / sys_permission / sys_user_role / sys_role_permission
product / product_sku / product_barcode / product_category
warehouse / warehouse_area / warehouse_shelf / warehouse_location
stock / stock_log / stock_check / stock_check_item
sales_order / sales_order_item
outbound_order / outbound_order_item
return_order / return_order_item
file_record / operation_log / es_sync_task
```

---

## 待引入依赖（按优先级）

1. Spring Security + JWT — 用户认证
2. PageHelper 或 MyBatis 分页插件 — 列表分页
3. Knife4j / SpringDoc — API 文档
4. Elasticsearch Java Client — 订单搜索
5. Redis — Token 存储 / 缓存（可选）
6. Spring Task — 定时任务（ES 同步）

---

## 开发顺序

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

## 部署

- 局域网内部署，不对外暴露
- Docker Compose 管理 MySQL + Elasticsearch + Spring Boot
- 本地磁盘存储文件（/app/uploads）
- 客户端通过 `http://服务器IP:8080/api` 访问

---

## 详细设计文档

完整架构和模块设计见：`docs/backend-framework.md`

---

## 开发进度（2026-06-09）

### 已完成模块

| 步骤 | 模块 | API 数量 | 状态 |
|------|------|----------|------|
| 1-3 | 基础结构/数据库/用户登录 | - | ✅ |
| 4 | 商品/SKU/条码/分类 | 15个 | ✅ |
| 5 | 仓库/库区/货架/库位 | 20个 | ✅ |
| 6 | 库存管理+流水+盘点 | 6个 | ✅ |
| 7 | 订单管理 | 4个 | ✅ |
| 8 | 出库流程 | 5个 | ✅ |
| 9 | 退货流程 | 5个 | ✅ |
| 10 | ES搜索（MySQL模拟） | 1个 | ✅ |
| 11 | 文件上传 | 3个 | ✅ |
| 12 | 操作日志 | 1个 | ✅ |
| 13 | 报表统计 | 3个 | ✅ |

**总计：63个API接口**

### API 接口清单

```text
认证相关：
  POST /api/auth/login                    登录
  POST /api/auth/register                 注册
  GET  /api/auth/profile                  获取当前用户信息

商品管理：
  GET    /api/products                    商品列表（分页）
  GET    /api/products/{id}               商品详情（含SKU）
  POST   /api/products                    新增商品
  PUT    /api/products                    修改商品
  DELETE /api/products/{id}               删除商品
  GET    /api/skus/product/{productId}    SKU列表
  GET    /api/skus/{id}                   SKU详情（含条码）
  GET    /api/skus/code/{skuCode}         按SKU编码查询
  POST   /api/skus                        新增SKU
  PUT    /api/skus                        修改SKU
  DELETE /api/skus/{id}                   删除SKU
  GET    /api/categories/tree             分类树
  POST   /api/categories                  新增分类
  PUT    /api/categories                  修改分类
  DELETE /api/categories/{id}             删除分类

仓库管理：
  GET    /api/warehouses                  仓库列表（分页）
  GET    /api/warehouses/{id}             仓库详情
  POST   /api/warehouses                  新增仓库
  PUT    /api/warehouses                  修改仓库
  DELETE /api/warehouses/{id}             删除仓库
  GET    /api/warehouse-areas/warehouse/{warehouseId}    库区列表
  GET    /api/warehouse-areas/{id}        库区详情
  POST   /api/warehouse-areas             新增库区
  PUT    /api/warehouse-areas             修改库区
  DELETE /api/warehouse-areas/{id}        删除库区
  GET    /api/warehouse-shelves/area/{areaId}    货架列表
  GET    /api/warehouse-shelves/{id}      货架详情
  POST   /api/warehouse-shelves           新增货架
  PUT    /api/warehouse-shelves           修改货架
  DELETE /api/warehouse-shelves/{id}      删除货架
  GET    /api/warehouse-locations/shelf/{shelfId}    库位列表
  GET    /api/warehouse-locations/{id}    库位详情
  POST   /api/warehouse-locations         新增库位
  PUT    /api/warehouse-locations         修改库位
  DELETE /api/warehouse-locations/{id}    删除库位

库存管理：
  GET  /api/stocks/query                  库存查询（分页）
  POST /api/stocks/adjust                 库存调整
  GET  /api/stock-logs                    库存流水列表
  POST /api/stock-checks                  创建盘点单
  GET  /api/stock-checks/{id}             盘点单详情
  POST /api/stock-checks/submit           提交盘点结果

订单管理：
  GET  /api/orders                        订单列表（分页）
  GET  /api/orders/{id}                   订单详情（含明细）
  POST /api/orders/import                 导入订单
  PUT  /api/orders/{id}/status            更新订单状态
  GET  /api/orders/search                 订单快速搜索

出库流程：
  GET  /api/outbound/list                 出库单列表（分页）
  GET  /api/outbound/{id}                 出库单详情
  POST /api/outbound/create               创建出库单
  POST /api/outbound/scan                 扫码核对
  POST /api/outbound/confirm              确认出库

退货流程：
  GET  /api/returns/list                  退货单列表（分页）
  GET  /api/returns/{id}                  退货单详情
  POST /api/returns/create                创建退货单
  POST /api/returns/check                 退货质检
  POST /api/returns/confirm               确认退货入库

文件上传：
  POST   /api/files/upload                上传文件
  GET    /api/files/{id}                  获取文件信息
  DELETE /api/files/{id}                  删除文件

操作日志：
  GET  /api/operation-logs                操作日志列表（分页）

报表统计：
  GET  /api/reports/dashboard             首页仪表盘数据
  GET  /api/reports/stock                 库存报表
  GET  /api/reports/outbound              出库报表

健康检查：
  GET  /api/health                        心跳接口
```

### 模块结构

```text
com.yiweilai.wms
 ├── common              Result, PageResult, Constants, HealthController
 ├── config              CorsConfig, MyBatisConfig, SecurityConfig, JacksonConfig, FileUploadConfig
 ├── exception           ErrorCode, BusinessException, GlobalExceptionHandler
 ├── security            JwtUtils, JwtAuthFilter
 ├── user                用户权限模块
 ├── product             商品管理模块（SPU/SKU/条码/分类）
 ├── warehouse           仓库库位模块（仓库/库区/货架/库位）
 ├── stock               库存管理模块（库存/流水/盘点）
 ├── order               订单管理模块
 ├── outbound            出库管理模块
 ├── returns             退货处理模块
 ├── search              ES搜索模块（当前MySQL模拟）
 ├── file                文件上传模块
 ├── log                 操作日志模块
 └── report              报表统计模块
```

### 待优化项

1. **ES搜索模块** - 当前使用MySQL模拟，后期引入Elasticsearch依赖后替换
2. **定时任务** - ES同步任务需要Spring Task支持
3. **API文档** - Knife4j已引入，需要配置启用
4. **缓存** - 可选引入Redis做Token存储和热点数据缓存

### 下一步

1. 执行建表SQL：`sql/schema.sql`
2. 配置数据库连接：`application.properties`
3. 启动项目测试
4. 接口测试（Postman 或 Knife4j）
