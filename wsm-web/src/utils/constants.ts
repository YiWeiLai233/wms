export const ORDER_STATUS_MAP: Record<string, { label: string; color: string; effect?: string }> = {
  WAIT_PAY: { label: '待付款', color: 'info' },
  WAIT_OUTBOUND: { label: '待出库', color: 'warning' },
  OUTBOUNDING: { label: '待发货', color: 'primary' },
  SHIPPED: { label: '已发货', color: 'success' },
  FINISHED: { label: '已完成', color: 'success', effect: 'dark' },
  CANCELLED: { label: '已取消', color: 'danger' },
  OUTBOUND_FAILED: { label: '出库失败', color: 'danger' },
  RETURNING: { label: '退货中', color: 'danger', effect: 'dark' },
  RETURNED: { label: '已退货', color: 'info', effect: 'dark' },
  PARTIAL_RETURNED: { label: '部分退货', color: 'warning' },
  EXCHANGING: { label: '换货中', color: 'purple' },
  EXCHANGED: { label: '已换货', color: 'purple', effect: 'dark' },
}

export const OUTBOUND_STATUS_MAP: Record<string, { label: string; color: string }> = {
  WAIT_PICKING: { label: '待发货', color: 'warning' },
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

export const EXCHANGE_STATUS_MAP: Record<string, { label: string; color: string }> = {
  PENDING_RETURN: { label: '待退回', color: 'info' },
  RETURNED: { label: '已退回', color: 'info' },
  CHECKED: { label: '已质检', color: 'info' },
  EXCHANGED: { label: '已换货', color: 'purple' },
  COMPLETED: { label: '已完成', color: 'success' },
  CANCELLED: { label: '已取消', color: 'danger' },
}

export const EXCHANGE_ITEM_TYPE_MAP: Record<string, { label: string; color: string }> = {
  RETURN_ITEM: { label: '退回商品', color: 'warning' },
  EXCHANGE_ITEM: { label: '换出商品', color: 'success' },
}

export const AREA_TYPE_MAP: Record<number, { label: string; color: string }> = {
  1: { label: '普通区', color: 'success' },
  2: { label: '退货区', color: 'warning' },
  3: { label: '次品区', color: 'danger' },
}

export const STOCK_BIZ_TYPE_MAP: Record<string, { label: string; color: string; className?: string }> = {
  INBOUND: { label: '入库', color: 'success' },
  OUTBOUND: { label: '出库', color: 'primary' },
  RETURN: { label: '退货', color: 'warning' },
  EXCHANGE: { label: '换货', color: 'info', className: 'exchange-stock-tag' },
  ADJUST: { label: '调整', color: 'info' },
  LOCK: { label: '锁定', color: 'danger' },
  RELEASE: { label: '释放', color: '' },
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
  { title: '工作台', icon: 'Odometer', path: '/dashboard' },

  {
    title: '商品中心',
    icon: 'ShoppingBag',
    children: [
      { title: '商品管理', path: '/product/list' },
      { title: 'SKU 管理', path: '/product/sku' },
      { title: '平台管理', path: '/system/platforms' },
    ],
  },

  {
    title: '仓库与库存',
    icon: 'House',
    children: [
      { title: '仓库管理', path: '/warehouse/list' },
      { title: '货架管理', path: '/warehouse/shelves' },
      { title: '特殊仓库', path: '/warehouse/special' },
      { title: '库存查询', path: '/stock/query' },
      { title: '库存流水', path: '/stock/log' },
      { title: '库存盘点', path: '/stock/check' },
      { title: '库存预警', path: '/system/stock-alert' },
      { title: '预警模板', path: '/system/stock-alert-template' },
    ],
  },

  {
    title: '订单中心',
    icon: 'Document',
    children: [
      { title: '订单管理', path: '/order/list' },
      { title: '发货管理', path: '/outbound/list' },
      { title: '退货管理', path: '/returns/list' },
      { title: '换货管理', path: '/exchange/list' },
    ],
  },

  {
    title: '快递物流',
    icon: 'Van',
    children: [
      { title: '快递查询', path: '/express/query' },
      { title: '快递公司', path: '/express/companies' },
      { title: '费用模板', path: '/express/fee-templates' },
      { title: '费用统计', path: '/express/fee-report' },
    ],
  },

  {
    title: '智能助手',
    icon: 'ChatDotRound',
    children: [
      { title: 'AI 助手', path: '/ai/assistant' },
      { title: '知识库管理', path: '/ai/knowledge' },
    ],
  },

  {
    title: '系统设置',
    icon: 'Setting',
    children: [
      { title: '用户管理', path: '/system/users' },
      { title: '操作日志', path: '/system/logs' },
      { title: '文件管理', path: '/system/files' },
      { title: '数据备份', path: '/system/backup' },
    ],
  },
]
