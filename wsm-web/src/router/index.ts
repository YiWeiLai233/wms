import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'

const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/login/LoginView.vue'),
    meta: { title: '登录', hideLayout: true },
  },
  {
    path: '/',
    component: () => import('@/layouts/DefaultLayout.vue'),
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/dashboard/DashboardView.vue'),
        meta: { title: '仪表盘', icon: 'Odometer' },
      },
      // 智能助手
      {
        path: 'ai/assistant',
        name: 'AiAssistant',
        component: () => import('@/views/ai/AiAssistantView.vue'),
        meta: { title: 'AI 助手', icon: 'ChatDotRound' },
      },
      {
        path: 'ai/knowledge',
        name: 'KnowledgeBase',
        component: () => import('@/views/ai/KnowledgeBaseView.vue'),
        meta: { title: '知识库管理', icon: 'Collection' },
      },
      // 仓库管理
      {
        path: 'warehouse/list',
        name: 'WarehouseList',
        component: () => import('@/views/warehouse/WarehouseList.vue'),
        meta: { title: '仓库管理', icon: 'House' },
      },
      {
        path: 'warehouse/shelves',
        name: 'ShelfList',
        component: () => import('@/views/warehouse/ShelfList.vue'),
        meta: { title: '货架管理', icon: 'Box' },
      },
      {
        path: 'warehouse/special',
        name: 'SpecialWarehouse',
        component: () => import('@/views/warehouse/SpecialWarehouseView.vue'),
        meta: { title: '特殊仓库管理', icon: 'Warning' },
      },
      // 商品管理
      {
        path: 'product/list',
        name: 'ProductList',
        component: () => import('@/views/product/ProductList.vue'),
        meta: { title: '商品管理', icon: 'ShoppingBag' },
      },
      {
        path: 'product/sku',
        name: 'SkuList',
        component: () => import('@/views/product/SkuList.vue'),
        meta: { title: 'SKU 管理', icon: 'PriceTag' },
      },
      // 库存管理
      {
        path: 'stock/query',
        name: 'StockQuery',
        component: () => import('@/views/stock/StockQuery.vue'),
        meta: { title: '库存查询', icon: 'Search' },
      },
      {
        path: 'stock/log',
        name: 'StockLog',
        component: () => import('@/views/stock/StockLog.vue'),
        meta: { title: '库存流水', icon: 'List' },
      },
      {
        path: 'stock/check',
        name: 'StockCheck',
        component: () => import('@/views/stock/StockCheck.vue'),
        meta: { title: '盘点管理', icon: 'DocumentChecked' },
      },
      // 系统管理
      {
        path: 'system/users',
        name: 'UserList',
        component: () => import('@/views/system/UserList.vue'),
        meta: { title: '用户管理', icon: 'UserFilled' },
      },
      {
        path: 'system/files',
        name: 'FileUpload',
        component: () => import('@/views/system/FileUpload.vue'),
        meta: { title: '文件管理', icon: 'FolderOpened' },
      },
      {
        path: 'system/logs',
        name: 'OperationLog',
        component: () => import('@/views/system/OperationLog.vue'),
        meta: { title: '操作日志', icon: 'Notebook' },
      },
      // 订单管理
      {
        path: 'order/list',
        name: 'OrderList',
        component: () => import('@/views/order/OrderList.vue'),
        meta: { title: '订单管理', icon: 'Document' },
      },
      // 出库管理
      {
        path: 'outbound/list',
        name: 'OutboundList',
        component: () => import('@/views/outbound/OutboundList.vue'),
        meta: { title: '出库管理', icon: 'TopRight' },
      },
      // 退货管理
      {
        path: 'returns/list',
        name: 'ReturnList',
        component: () => import('@/views/returns/ReturnList.vue'),
        meta: { title: '退货管理', icon: 'BottomLeft' },
      },
      // 快递管理
      {
        path: 'express/query',
        name: 'ExpressQuery',
        component: () => import('@/views/express/ExpressQuery.vue'),
        meta: { title: '快递查询', icon: 'Van' },
      },
      {
        path: 'express/companies',
        name: 'ExpressCompanyList',
        component: () => import('@/views/express/ExpressCompanyList.vue'),
        meta: { title: '快递公司管理', icon: 'OfficeBuilding' },
      },
      {
        path: 'express/fee-templates',
        name: 'ExpressFeeTemplate',
        component: () => import('@/views/express/ExpressFeeTemplate.vue'),
        meta: { title: '费用模板管理', icon: 'Money' },
      },
      {
        path: 'express/fee-report',
        name: 'ExpressFeeReport',
        component: () => import('@/views/express/ExpressFeeReport.vue'),
        meta: { title: '快递费用统计', icon: 'DataAnalysis' },
      },
    ],
  },
  {
    path: '/:pathMatch(.*)*',
    redirect: '/dashboard',
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

// 路由守卫：未登录跳转登录页
router.beforeEach((to, _from, next) => {
  const token = localStorage.getItem('token')
  if (to.path !== '/login' && !token) {
    next('/login')
  } else if (to.path === '/login' && token) {
    next('/dashboard')
  } else {
    next()
  }
})

export default router
