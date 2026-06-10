# WMS 项目接口分析报告

> 分析时间：2026-06-10
> 分析范围：后端 Controller 接口 vs 前端 API 调用

---

## 一、后端接口总览（20个 Controller）

| 模块 | Controller | 接口数量 | 前端调用状态 |
|------|-----------|---------|-------------|
| 健康检查 | HealthController | 1 | ✅ 未调用（正常） |
| 商品管理 | ProductController | 5 | ✅ 全部使用 |
| 分类管理 | ProductCategoryController | 5 | ⚠️ 部分使用 |
| SKU管理 | ProductSkuController | 7 | ✅ 全部使用 |
| 仓库管理 | WarehouseController | 5 | ⚠️ 部分使用 |
| 库区管理 | WarehouseAreaController | 5 | ⚠️ 部分使用 |
| 货架管理 | WarehouseShelfController | 5 | ⚠️ 部分使用 |
| 库位管理 | WarehouseLocationController | 5 | ⚠️ 部分使用 |
| 库存管理 | StockController | 2 | ✅ 全部使用 |
| 库存流水 | StockLogController | 1 | ✅ 全部使用 |
| 盘点管理 | StockCheckController | 3 | ✅ 全部使用 |
| 订单管理 | OrderController | 4 | ✅ 全部使用 |
| 订单搜索 | OrderSearchController | 1 | ⚠️ 未使用 |
| 出库管理 | OutboundController | 5 | ✅ 全部使用 |
| 退货管理 | ReturnController | 5 | ✅ 全部使用 |
| 用户管理 | UserController | 9 | ✅ 全部使用 |
| 文件管理 | FileController | 3 | ⚠️ 部分使用 |
| 操作日志 | OperationLogController | 1 | ✅ 全部使用 |
| 报表统计 | ReportController | 3 | ⚠️ 部分使用 |
| OCR识别 | OcrController | 4 | ❌ 完全未使用 |

---

## 二、🔴 无用接口（前端定义了但未使用）

### 2.1 订单搜索接口

| 项目 | 内容 |
|------|------|
| **API 函数** | `searchOrders` |
| **文件位置** | `wsm-web/src/api/order.ts:63` |
| **对应后端接口** | `GET /api/orders/search` |
| **状态** | ⚠️ 未在任何视图组件中调用 |
| **建议** | 删除或在订单页面中使用此搜索功能 |

```typescript
// 当前定义但未使用
export function searchOrders(params: PageParams & { keyword?: string; orderStatus?: string; warehouseId?: number; startTime?: string; endTime?: string }) {
  return request.get<any, ApiResponse<PageResult<Order>>>('/orders/search', { params })
}
```

### 2.2 分类创建接口

| 项目 | 内容 |
|------|------|
| **API 函数** | `createCategory` |
| **文件位置** | `wsm-web/src/api/product.ts:98` |
| **对应后端接口** | `POST /api/categories` |
| **状态** | ⚠️ 未在任何视图组件中调用 |
| **建议** | 删除或在分类管理页面中使用 |

```typescript
// 当前定义但未使用
export function createCategory(data: Partial<Category>) {
  return request.post<any, ApiResponse<number>>('/categories', data)
}
```

### 2.3 库存报表接口

| 项目 | 内容 |
|------|------|
| **API 函数** | `getStockReport` |
| **文件位置** | `wsm-web/src/api/report.ts:36` |
| **对应后端接口** | `GET /api/reports/stock` |
| **状态** | ⚠️ 未在任何视图组件中调用 |
| **建议** | 删除或在报表页面中使用 |

```typescript
// 当前定义但未使用
export function getStockReport() {
  return request.get<any, ApiResponse<StockReport>>('/reports/stock')
}
```

### 2.4 出库报表接口

| 项目 | 内容 |
|------|------|
| **API 函数** | `getOutboundReport` |
| **文件位置** | `wsm-web/src/api/report.ts:41` |
| **对应后端接口** | `GET /api/reports/outbound` |
| **状态** | ⚠️ 未在任何视图组件中调用 |
| **建议** | 删除或在报表页面中使用 |

```typescript
// 当前定义但未使用
export function getOutboundReport() {
  return request.get<any, ApiResponse<OutboundReport>>('/reports/outbound')
}
```

---

## 三、🔴 后端有但前端完全缺失的接口

### 3.1 OCR 识别模块（4个接口）

**问题严重程度：高** - 后端有完整的 OCR 功能实现，但前端没有任何 API 定义和调用，功能完全未使用。

| 接口路径 | 方法 | 功能说明 |
|----------|------|---------|
| `/api/ocr/express` | POST | 快递单识别（Base64） |
| `/api/ocr/express/upload` | POST | 快递单识别（文件上传） |
| `/api/ocr/general` | POST | 通用文字识别（Base64） |
| `/api/ocr/general/upload` | POST | 通用文字识别（文件上传） |

**后端实现位置**：`wms-backend/src/main/java/com/yiweilai/wms/ocr/controller/OcrController.java`

**建议**：
- 方案A：前端添加 OCR 识别页面，实现快递单和文字识别功能
- 方案B：如果 OCR 功能不需要，删除后端 OcrController 及相关代码

### 3.2 健康检查接口

| 接口路径 | 方法 | 功能说明 |
|----------|------|---------|
| `/api/health` | GET | 返回 "helloWms" 用于健康检查 |

**说明**：此接口通常用于服务器监控和负载均衡器健康检查，前端不调用是合理的，建议保留。

---

## 四、🟡 前端缺失的 API 定义

### 4.1 分类管理缺失接口

| 后端接口 | 方法 | 前端状态 | 建议 |
|----------|------|---------|------|
| `/api/categories` | PUT | 未定义 `updateCategory` | 需要添加 |
| `/api/categories/{id}` | DELETE | 未定义 `deleteCategory` | 需要添加 |

**影响**：当前分类管理只能查看和创建，无法编辑和删除分类。

### 4.2 仓库相关详情查询接口

| 后端接口 | 方法 | 前端状态 | 建议 |
|----------|------|---------|------|
| `/api/warehouses/{id}` | GET | 未定义 `getWarehouseDetail` | 按需添加 |
| `/api/warehouse-areas/{id}` | GET | 未定义 `getAreaDetail` | 按需添加 |
| `/api/warehouse-shelves/{id}` | GET | 未定义 `getShelfDetail` | 按需添加 |
| `/api/warehouse-locations/{id}` | GET | 未定义 `getLocationDetail` | 按需添加 |

**说明**：当前前端通过列表接口获取数据，详情接口可能用于编辑时回显数据。

### 4.3 文件管理缺失接口

| 后端接口 | 方法 | 前端状态 | 建议 |
|----------|------|---------|------|
| `/api/files/{id}` | GET | 已定义 `getFileInfo` 但未使用 | 按需使用 |

---

## 五、✅ 接口路径匹配检查

经检查，前端 API 路径与后端接口路径**基本匹配**，未发现明显的路径不匹配问题。

### 路径对照表

| 前端 API 路径 | 后端接口路径 | 匹配状态 |
|--------------|-------------|---------|
| `/products` | `/api/products` | ✅ |
| `/products/{id}` | `/api/products/{id}` | ✅ |
| `/categories/tree` | `/api/categories/tree` | ✅ |
| `/categories` | `/api/categories` | ✅ |
| `/skus` | `/api/skus` | ✅ |
| `/skus/{id}` | `/api/skus/{id}` | ✅ |
| `/skus/product/{productId}` | `/api/skus/product/{productId}` | ✅ |
| `/warehouses` | `/api/warehouses` | ✅ |
| `/warehouses/{id}` | `/api/warehouses/{id}` | ✅ |
| `/warehouse-areas/warehouse/{warehouseId}` | `/api/warehouse-areas/warehouse/{warehouseId}` | ✅ |
| `/warehouse-areas/{id}` | `/api/warehouse-areas/{id}` | ✅ |
| `/warehouse-shelves/warehouse/{warehouseId}` | `/api/warehouse-shelves/warehouse/{warehouseId}` | ✅ |
| `/warehouse-shelves/{id}` | `/api/warehouse-shelves/{id}` | ✅ |
| `/warehouse-locations/shelf/{shelfId}` | `/api/warehouse-locations/shelf/{shelfId}` | ✅ |
| `/warehouse-locations/{id}` | `/api/warehouse-locations/{id}` | ✅ |
| `/stocks/query` | `/api/stocks/query` | ✅ |
| `/stocks/adjust` | `/api/stocks/adjust` | ✅ |
| `/stock-logs` | `/api/stock-logs` | ✅ |
| `/stock-checks` | `/api/stock-checks` | ✅ |
| `/stock-checks/{id}` | `/api/stock-checks/{id}` | ✅ |
| `/stock-checks/submit` | `/api/stock-checks/submit` | ✅ |
| `/orders` | `/api/orders` | ✅ |
| `/orders/{id}` | `/api/orders/{id}` | ✅ |
| `/orders/import` | `/api/orders/import` | ✅ |
| `/orders/{id}/status` | `/api/orders/{id}/status` | ✅ |
| `/orders/search` | `/api/orders/search` | ✅ |
| `/outbound/list` | `/api/outbound/list` | ✅ |
| `/outbound/{id}` | `/api/outbound/{id}` | ✅ |
| `/outbound/create` | `/api/outbound/create` | ✅ |
| `/outbound/scan` | `/api/outbound/scan` | ✅ |
| `/outbound/confirm` | `/api/outbound/confirm` | ✅ |
| `/returns/list` | `/api/returns/list` | ✅ |
| `/returns/{id}` | `/api/returns/{id}` | ✅ |
| `/returns/create` | `/api/returns/create` | ✅ |
| `/returns/check` | `/api/returns/check` | ✅ |
| `/returns/confirm` | `/api/returns/confirm` | ✅ |
| `/auth/login` | `/api/auth/login` | ✅ |
| `/auth/profile` | `/api/auth/profile` | ✅ |
| `/users` | `/api/users` | ✅ |
| `/users/{id}` | `/api/users/{id}` | ✅ |
| `/users/{id}/reset-password` | `/api/users/{id}/reset-password` | ✅ |
| `/roles` | `/api/roles` | ✅ |
| `/files/upload` | `/api/files/upload` | ✅ |
| `/files/{id}` | `/api/files/{id}` | ✅ |
| `/operation-logs` | `/api/operation-logs` | ✅ |
| `/reports/dashboard` | `/api/reports/dashboard` | ✅ |
| `/reports/stock` | `/api/reports/stock` | ✅ |
| `/reports/outbound` | `/api/reports/outbound` | ✅ |

---

## 六、📋 建议处理方案

### 优先级 1：清理无用代码

| 序号 | 任务 | 涉及文件 | 预计工作量 |
|------|------|---------|-----------|
| 1 | 删除未使用的 `searchOrders` API | `wsm-web/src/api/order.ts` | 5分钟 |
| 2 | 删除未使用的 `createCategory` API | `wsm-web/src/api/product.ts` | 5分钟 |
| 3 | 处理未使用的 `getStockReport` 和 `getOutboundReport` | `wsm-web/src/api/report.ts` | 10分钟 |

### 优先级 2：完善功能

| 序号 | 任务 | 涉及文件 | 预计工作量 |
|------|------|---------|-----------|
| 1 | 决定 OCR 功能去留 | 后端 OcrController 或前端新增页面 | 2-4小时 |
| 2 | 添加分类管理的修改/删除功能 | 前端 API + 分类管理页面 | 1-2小时 |

### 优先级 3：按需优化

| 序号 | 任务 | 涉及文件 | 预计工作量 |
|------|------|---------|-----------|
| 1 | 添加详情查询接口（如需要） | 前端 API 文件 | 30分钟 |
| 2 | 实现报表页面功能 | 前端新增报表页面 | 2-3小时 |

---

## 七、统计摘要

| 类别 | 数量 |
|------|------|
| 后端接口总数 | 78 |
| 前端 API 定义总数 | 48 |
| 已使用的前端 API | 44 |
| 未使用的前端 API | 4 |
| 后端有但前端缺失 | 6 |
| 路径不匹配 | 0 |

---

## 附录：相关文件路径

### 后端 Controller 文件
```
wms-backend/src/main/java/com/yiweilai/wms/
├── common/HealthController.java
├── product/controller/
│   ├── ProductController.java
│   ├── ProductCategoryController.java
│   └── ProductSkuController.java
├── warehouse/controller/
│   ├── WarehouseController.java
│   ├── WarehouseAreaController.java
│   ├── WarehouseShelfController.java
│   └── WarehouseLocationController.java
├── stock/controller/
│   ├── StockController.java
│   ├── StockLogController.java
│   └── StockCheckController.java
├── order/controller/OrderController.java
├── search/controller/OrderSearchController.java
├── outbound/controller/OutboundController.java
├── returns/controller/ReturnController.java
├── user/controller/UserController.java
├── file/controller/FileController.java
├── log/controller/OperationLogController.java
├── report/controller/ReportController.java
└── ocr/controller/OcrController.java
```

### 前端 API 文件
```
wsm-web/src/api/
├── auth.ts
├── file.ts
├── log.ts
├── order.ts
├── outbound.ts
├── product.ts
├── report.ts
├── request.ts
├── returns.ts
├── stock.ts
├── user.ts
└── warehouse.ts
```
