export const ORDER_STATUS_MAP: Record<string, { label: string; color: string; effect?: string }> = {
  WAIT_PAY: { label: '待付款', color: 'info' },
  WAIT_OUTBOUND: { label: '待出库', color: 'warning' },
  OUTBOUNDING: { label: '出库中', color: 'primary' },
  SHIPPED: { label: '已发货', color: 'success' },
  FINISHED: { label: '已完成', color: 'success', effect: 'dark' },
  CANCELLED: { label: '已取消', color: 'danger' },
  OUTBOUND_FAILED: { label: '出库失败', color: 'danger' },
  RETURNING: { label: '退货中', color: 'danger', effect: 'dark' },
  RETURNED: { label: '已退货', color: 'info', effect: 'dark' },
}

export const OUTBOUND_STATUS_MAP: Record<string, { label: string; color: string }> = {
  WAIT_PICKING: { label: '待拣货', color: 'warning' },
  PICKING: { label: '拣货中', color: '' },
  PICKED: { label: '已拣货', color: 'success' },
  SHIPPED: { label: '已发货', color: 'success' },
  CANCELLED: { label: '已取消', color: 'danger' },
}

export const RETURN_STATUS_MAP: Record<string, { label: string; color: string }> = {
  PENDING_CHECK: { label: '待质检', color: 'warning' },
  SELLABLE: { label: '可售待入库', color: 'success' },
  DEFECTIVE: { label: '次品待入库', color: 'warning' },
  SCRAPPED: { label: '报废待处理', color: 'danger' },
  COMPLETED: { label: '已入库', color: 'success' },
  CHECKED: { label: '已质检', color: '' },
  CONFIRMED: { label: '已入库', color: 'success' },
  CANCELLED: { label: '已取消', color: 'danger' },
}

export const QUALITY_STATUS_MAP: Record<string, { label: string; color: string }> = {
  SELLABLE: { label: '可售', color: 'success' },
  DEFECTIVE: { label: '次品', color: 'warning' },
  SCRAPPED: { label: '报废', color: 'danger' },
}

export const AREA_TYPE_MAP: Record<number, { label: string; color: string }> = {
  1: { label: '普通区', color: 'success' },
  2: { label: '退货区', color: 'warning' },
  3: { label: '次品区', color: 'danger' },
}

export const STOCK_BIZ_TYPE_MAP: Record<string, { label: string; color: string }> = {
  INBOUND: { label: '入库', color: 'success' },
  OUTBOUND: { label: '出库', color: 'danger' },
  RETURN: { label: '退货', color: 'warning' },
  ADJUST: { label: '调整', color: 'info' },
}

export const STATUS_MAP: Record<number, { label: string; color: string }> = {
  0: { label: '禁用', color: 'danger' },
  1: { label: '启用', color: 'success' },
}

export interface MenuItem {
  title: string
  icon?: string
  path?: string
  children?: MenuItem[]
}

export const MENU_LIST: MenuItem[] = [
  { title: '仪表盘', icon: 'Odometer', path: '/dashboard' },
  {
    title: '仓库管理',
    icon: 'House',
    children: [
      { title: '仓库管理', path: '/warehouse/list' },
      { title: '货架管理', path: '/warehouse/shelves' },
    ],
  },
  {
    title: '商品管理',
    icon: 'ShoppingBag',
    children: [
      { title: '商品列表', path: '/product/list' },
      { title: 'SKU 管理', path: '/product/sku' },
    ],
  },
  {
    title: '库存管理',
    icon: 'Search',
    children: [
      { title: '库存查询', path: '/stock/query' },
      { title: '库存流水', path: '/stock/log' },
      { title: '盘点管理', path: '/stock/check' },
    ],
  },
  {
    title: '订单中心',
    icon: 'Document',
    children: [
      { title: '订单管理', path: '/order/list' },
      { title: '出库管理', path: '/outbound/list' },
      { title: '退货管理', path: '/returns/list' },
    ],
  },
  {
    title: '快递管理',
    icon: 'Van',
    children: [
      { title: '快递查询', path: '/express/query' },
      { title: '快递公司管理', path: '/express/companies' },
      { title: '费用模板管理', path: '/express/fee-templates' },
      { title: '快递费用统计', path: '/express/fee-report' },
    ],
  },
  {
    title: '系统管理',
    icon: 'Setting',
    children: [
      { title: '用户管理', path: '/system/users' },
      { title: '文件管理', path: '/system/files' },
      { title: '操作日志', path: '/system/logs' },
    ],
  },
]
