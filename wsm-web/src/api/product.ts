import request from './request'
import type { ApiResponse, PageParams, PageResult } from './request'

export interface Product {
  id: number
  spuCode: string
  name: string
  shelfId: number
  shelfCode: string
  shelfName: string
  categoryName: string
  mainImage: string
  description: string
  price: number
  status: number
  createdAt: string
  updatedAt: string
  skuList?: ProductSizeSku[]
}

export interface ProductSizeSku {
  sizeValue: string
  skuCode?: string
  name?: string
  quantity?: number
  costPrice?: number
  salePrice?: number
  weight?: number
  volume?: number
  image?: string
}

export interface Sku {
  id: number
  productId: number
  shelfId: number
  shelfCode: string
  skuCode: string
  name: string
  sizeValue: string
  quantity: number
  availableQty: number
  lockedQty: number
  totalQty: number
  initialQuantity?: number
  warehouseId?: number
  inboundRemark?: string
  costPrice: number
  salePrice: number
  weight: number
  volume?: number
  image?: string
  status: number
  barcodeList?: { id: number; skuId: number; barcode: string }[]
}

export interface Category {
  id: number
  name: string
  parentId: number
  sortOrder: number
  status: number
  children?: Category[]
}

// 商品列表
export function getProductList(params: PageParams & { keyword?: string; categoryId?: number; status?: number }) {
  return request.get<any, ApiResponse<PageResult<Product>>>('/products', { params })
}

// 商品详情
export function getProductDetail(id: number) {
  return request.get<any, ApiResponse<Product & { skuList: Sku[] }>>(`/products/${id}`)
}

// 新增商品
export function createProduct(data: Partial<Product>) {
  return request.post<any, ApiResponse<number>>('/products', data)
}

// 修改商品
export function updateProduct(data: Partial<Product>) {
  return request.put<any, ApiResponse<void>>('/products', data)
}

// 删除商品
export function deleteProduct(id: number) {
  return request.delete<any, ApiResponse<void>>(`/products/${id}`)
}

// SKU 列表（按商品）
export function getSkuList(productId: number) {
  return request.get<any, ApiResponse<Sku[]>>(`/skus/product/${productId}`)
}

// 所有 SKU 列表
export function getAllSkuList(params?: PageParams & { keyword?: string; status?: number; warehouseId?: number }) {
  return request.get<any, ApiResponse<PageResult<Sku & { productName?: string; shelfCode?: string; categoryName?: string }>>>('/skus', { params })
}

// 新增 SKU
export function createSku(data: Partial<Sku>) {
  return request.post<any, ApiResponse<number>>('/skus', data)
}

// 修改 SKU
export function updateSku(data: Partial<Sku>) {
  return request.put<any, ApiResponse<void>>('/skus', data)
}

// 删除 SKU
export function deleteSku(id: number) {
  return request.delete<any, ApiResponse<void>>(`/skus/${id}`)
}

// 分类树
export function getCategoryTree() {
  return request.get<any, ApiResponse<Category[]>>('/categories/tree')
}

// 新增分类
export function createCategory(data: Partial<Category>) {
  return request.post<any, ApiResponse<number>>('/categories', data)
}
