import dayjs from 'dayjs'

/**
 * 格式化日期时间
 */
export function formatDateTime(date: string | Date, format = 'YYYY-MM-DD HH:mm:ss') {
  if (!date) return '-'
  return dayjs(date).format(format)
}

/**
 * 格式化日期
 */
export function formatDate(date: string | Date) {
  return formatDateTime(date, 'YYYY-MM-DD')
}

/**
 * 格式化金额（保留两位小数）
 */
export function formatMoney(amount: number | undefined | null) {
  if (amount == null) return '¥0.00'
  return `¥${Number(amount).toFixed(2)}`
}

/**
 * 格式化数量
 */
export function formatQuantity(qty: number | undefined | null) {
  if (qty == null) return '0'
  return String(qty)
}
