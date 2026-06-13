import request from './request'
import type { ApiResponse, PageParams, PageResult } from './request'

// ==================== 类型定义 ====================

export interface ExpressCompany {
  id: number
  name: string
  code: string
  contact?: string
  phone?: string
  status: number
  createdAt?: string
}

export interface ExpressFeeStep {
  id?: number
  minWeight: number
  maxWeight: number
  fee: number
  sortOrder?: number
}

export interface ExpressFeeTemplate {
  id: number
  companyId: number
  companyName?: string
  name: string
  isDefault: number
  status: number
  remark?: string
  createdAt?: string
  steps?: ExpressFeeStep[]
}

// ==================== 快递公司 API ====================

/** 查询所有启用的公司（下拉选择） */
export function getCompanyList() {
  return request.get<any, ApiResponse<ExpressCompany[]>>('/express/companies')
}

/** 分页查询公司 */
export function getCompanyPage(params: PageParams & { name?: string; status?: number }) {
  return request.get<any, ApiResponse<PageResult<ExpressCompany>>>('/express/companies/page', { params })
}

/** 新增公司 */
export function createCompany(data: Partial<ExpressCompany>) {
  return request.post<any, ApiResponse<number>>('/express/companies', data)
}

/** 修改公司 */
export function updateCompany(data: Partial<ExpressCompany>) {
  return request.put<any, ApiResponse<void>>('/express/companies', data)
}

/** 删除公司 */
export function deleteCompany(id: number) {
  return request.delete<any, ApiResponse<void>>(`/express/companies/${id}`)
}

// ==================== 费用模板 API ====================

/** 查询公司下的模板列表 */
export function getTemplateListByCompany(companyId: number) {
  return request.get<any, ApiResponse<ExpressFeeTemplate[]>>(`/express/fee-templates/company/${companyId}`)
}

/** 分页查询模板 */
export function getTemplatePage(params: PageParams & { companyId?: number; name?: string }) {
  return request.get<any, ApiResponse<PageResult<ExpressFeeTemplate>>>('/express/fee-templates', { params })
}

/** 查询模板详情（含阶梯） */
export function getTemplateDetail(id: number) {
  return request.get<any, ApiResponse<ExpressFeeTemplate>>(`/express/fee-templates/${id}`)
}

/** 查询默认模板 */
export function getDefaultTemplate() {
  return request.get<any, ApiResponse<ExpressFeeTemplate>>('/express/fee-templates/default')
}

/** 新增模板 */
export function createTemplate(data: { companyId: number; name: string; isDefault?: number; remark?: string; steps: ExpressFeeStep[] }) {
  return request.post<any, ApiResponse<number>>('/express/fee-templates', data)
}

/** 修改模板 */
export function updateTemplate(data: { id: number; companyId?: number; name?: string; isDefault?: number; remark?: string; steps: ExpressFeeStep[] }) {
  return request.put<any, ApiResponse<void>>('/express/fee-templates', data)
}

/** 删除模板 */
export function deleteTemplate(id: number) {
  return request.delete<any, ApiResponse<void>>(`/express/fee-templates/${id}`)
}

// ==================== 费用计算 API ====================

/** 计算快递费用 */
export function calculateFee(data: { totalWeight: number; templateId?: number }) {
  return request.post<any, ApiResponse<any>>('/express/calculate-fee', data)
}
