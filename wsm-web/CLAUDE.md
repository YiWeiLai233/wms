# WSM 仓库管理系统 - 前端

> 拖鞋行业智能仓储解决方案

## 技术栈

- Vue 3.5 + Vite 6 + TypeScript 5.7
- Element Plus 2.9 (UI 组件库)
- UnoCSS (原子化 CSS)
- Pinia (状态管理)
- Vue Router 4 (路由)
- Axios (HTTP 请求)
- ECharts + vue-echarts (图表)
- dayjs (日期处理)

## 项目结构

```
src/
├── main.ts                         # 应用入口
├── App.vue                         # 根组件
├── api/                            # API 请求模块
│   ├── request.ts                  # Axios 实例 + JWT 拦截器 + 错误处理
│   ├── auth.ts                     # 认证（登录/用户信息）
│   ├── warehouse.ts                # 仓库/库区/货架/库位 CRUD
│   ├── stock.ts                    # 库存查询/调整/流水/盘点
│   ├── user.ts                     # 用户管理/角色列表/重置密码
│   ├── product.ts                  # 商品/分类/SKU 管理
│   ├── order.ts                    # 订单 CRUD/导入/状态更新
│   ├── outbound.ts                 # 出库单管理/扫码/确认出库
│   ├── returns.ts                  # 退货单管理/质检/确认入库
│   ├── report.ts                   # 仪表盘/库存报表/出库报表
│   ├── user.ts                     # 用户管理/角色列表/重置密码
│   ├── file.ts                     # 文件上传/获取/删除
│   └── log.ts                      # 操作日志列表
├── assets/styles/                  # 全局样式
│   └── global.scss                 # 样式重置 + Element Plus 覆盖
├── components/                     # 通用组件
│   ├── StatCard.vue                # 数据统计卡片
│   ├── PageHeader.vue              # 页面标题 + 操作按钮区
│   └── EmptyState.vue              # 空状态占位
├── composables/                    # 组合式函数
│   ├── useTable.ts                 # 表格分页/搜索/重置通用逻辑
│   └── usePermission.ts            # 角色权限判断
├── layouts/
│   └── DefaultLayout.vue           # 主布局（侧边栏+顶栏+内容区）
├── router/
│   └── index.ts                    # 路由配置（13个子路由 + 登录守卫）
├── stores/
│   ├── user.ts                     # 用户/认证状态（含 mock 兜底）
│   └── app.ts                      # 全局应用状态（侧边栏折叠）
├── utils/
│   ├── constants.ts                # 状态枚举映射 + 侧边栏菜单配置
│   └── format.ts                   # 日期/金额格式化
└── views/                          # 页面视图
    ├── login/LoginView.vue         # 登录页
    ├── dashboard/DashboardView.vue # 仪表盘（统计卡片 + ECharts 图表）
    ├── warehouse/
    │   ├── WarehouseList.vue       # 仓库管理
    │   ├── AreaList.vue            # 库区管理
    │   ├── ShelfList.vue           # 货架管理
    │   └── LocationList.vue        # 库位管理
    ├── product/
    │   ├── ProductList.vue         # 商品管理
    │   ├── SkuList.vue             # SKU 管理
    │   └── CategoryTree.vue        # 分类管理
    ├── stock/
    │   ├── StockQuery.vue          # 库存查询
    │   ├── StockLog.vue            # 库存流水
    │   └── StockCheck.vue          # 盘点管理
    ├── order/OrderList.vue         # 订单管理
    ├── outbound/OutboundList.vue   # 出库管理
    ├── returns/ReturnList.vue      # 退货管理
    └── system/
        ├── UserList.vue            # 用户管理
        ├── FileUpload.vue          # 文件管理
        └── OperationLog.vue        # 操作日志
```

## 启动命令

```bash
npm install        # 安装依赖
npm run dev        # 开发服务器 http://localhost:5173
npm run build      # 生产构建
npm run preview    # 预览构建产物
```

## 后端 API

- 基础路径：`/api`（开发环境代理到 `http://localhost:8080`）
- 认证方式：JWT Token（`Authorization: Bearer {token}`）
- 响应格式：`{ code: 200, message: "success", data: {} }`
- 分页格式：`{ total, list, pageNum, pageSize }`
- 详细文档：见项目根目录 `api-doc.md`

## 登录

- 默认账号：`admin` / `123456`
- 后端不可用时自动使用 mock 数据登录（Store 兜底）

## 设计规范

- 主色调：深蓝灰 `#1e293b`（侧边栏）+ 蓝色 `#3b82f6`（强调色）
- 内容区背景：`#f1f5f9`
- 侧边栏可折叠：220px ↔ 64px
- 表格：斑马纹 + 紧凑行高
- 状态标签：不同状态对应不同 Element Plus Tag 颜色

## 业务模块

| 模块 | 页面 | 功能 |
|------|------|------|
| 仪表盘 | DashboardView | 4 个统计卡片 + 订单趋势折线图 + 状态分布饼图 |
| 仓库管理 | WarehouseList / AreaList / ShelfList / LocationList | 四级仓库结构 CRUD（仓库→库区→货架→库位） |
| 商品管理 | ProductList / SkuList / CategoryTree | 商品 CRUD + SKU 管理 + 分类树 |
| 库存管理 | StockQuery / StockLog / StockCheck | 库存查询 + 流水记录 + 盘点管理 |
| 订单管理 | OrderList | 订单列表 + 导入 + 详情 + 创建出库单 + 退货 |
| 出库管理 | OutboundList | 出库单列表 + 详情 + 确认出库 |
| 退货管理 | ReturnList | 退货单列表 + 详情 + 质检 + 确认入库 |
| 系统管理 | UserList / FileUpload / OperationLog | 用户管理 + 文件上传 + 操作日志 |

## 状态枚举

### 订单状态
WAIT_PAY → WAIT_OUTBOUND → OUTBOUNDING → SHIPPED → FINISHED
CANCELLED / RETURNING → RETURNED

### 出库单状态
WAIT_PICKING → PICKING → PICKED → SHIPPED

### 退货单状态
PENDING_CHECK → CHECKED → CONFIRMED

### 质检结果
SELLABLE（可售）/ DEFECTIVE（次品）/ SCRAPPED（报废）

## 开发规范

- 使用 Composition API (`<script setup>`)
- 表格页面统一使用 `useTable` composable
- API 函数统一在 `src/api/` 模块中定义，返回类型明确
- 状态枚举统一在 `src/utils/constants.ts` 中定义
- 组件命名采用 PascalCase，文件名与组件名一致
