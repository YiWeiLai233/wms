# WMS 后端 API 接口文档

> 基础路径：`http://localhost:8080/api`
>
> 认证方式：JWT Token（Header: `Authorization: Bearer {token}`）

---

## 通用说明

### 统一响应格式

```json
{
  "code": 200,
  "message": "success",
  "data": {}
}
```

### 分页响应格式

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "total": 100,
    "list": [],
    "pageNum": 1,
    "pageSize": 10
  }
}
```

### 通用分页参数

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| page | Integer | 否 | 页码，默认1 |
| size | Integer | 否 | 每页数量，默认10 |

---

## 1. 认证模块

### 1.1 登录

**POST** `/api/auth/login`

**请求参数：**

```json
{
  "username": "admin",
  "password": "123456"
}
```

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| username | String | 是 | 用户名 |
| password | String | 是 | 密码 |

**响应示例：**

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "userId": 1,
    "username": "admin",
    "roles": ["SUPER_ADMIN"]
  }
}
```

### 1.2 获取当前用户信息

**GET** `/api/auth/profile`

**请求头：**

```
Authorization: Bearer {token}
```

**响应示例：**

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 1,
    "username": "admin",
    "realName": "管理员",
    "phone": "13800138000",
    "roles": [
      {
        "id": 1,
        "roleCode": "SUPER_ADMIN",
        "roleName": "超级管理员"
      }
    ]
  }
}
```

---

## 2. 商品管理

### 2.1 商品列表

**GET** `/api/products`

**请求参数：**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| page | Integer | 否 | 页码，默认1 |
| size | Integer | 否 | 每页数量，默认10 |
| keyword | String | 否 | 关键词（名称/编码） |
| categoryId | Long | 否 | 分类ID |
| status | Integer | 否 | 状态：0-禁用 1-启用 |

**响应示例：**

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "total": 50,
    "list": [
      {
        "id": 1,
        "spuCode": "SPU001",
        "name": "红色花朵鞋",
        "categoryId": 1,
        "categoryName": "鞋子",
        "brand": "Nike",
        "mainImage": "/uploads/product/xxx.jpg",
        "description": "时尚运动鞋",
        "price": 299.00,
        "status": 1,
        "createdAt": "2026-06-08T10:00:00",
        "updatedAt": "2026-06-08T10:00:00"
      }
    ]
  }
}
```

### 2.2 商品详情

**GET** `/api/products/{id}`

**路径参数：**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| id | Long | 是 | 商品ID |

**响应示例：**

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 1,
    "spuCode": "SPU001",
    "name": "红色花朵鞋",
    "categoryId": 1,
    "categoryName": "鞋子",
    "brand": "Nike",
    "mainImage": "/uploads/product/xxx.jpg",
    "description": "时尚运动鞋",
    "price": 299.00,
    "status": 1,
    "skuList": [
      {
        "id": 1,
        "productId": 1,
        "skuCode": "SKU001",
        "name": "红色花朵鞋-42码",
        "spec": "{\"颜色\":\"红\",\"尺码\":\"42\"}",
        "costPrice": 150.00,
        "salePrice": 299.00,
        "weight": 0.8,
        "status": 1,
        "barcodeList": [
          {
            "id": 1,
            "skuId": 1,
            "barcode": "6930000000011",
            "createdAt": "2026-06-08T10:00:00"
          }
        ]
      }
    ],
    "createdAt": "2026-06-08T10:00:00",
    "updatedAt": "2026-06-08T10:00:00"
  }
}
```

### 2.3 新增商品

**POST** `/api/products`

**请求参数：**

```json
{
  "spuCode": "SPU001",
  "name": "红色花朵鞋",
  "categoryId": 1,
  "brand": "Nike",
  "mainImage": "/uploads/product/xxx.jpg",
  "description": "时尚运动鞋",
  "price": 299.00,
  "status": 1
}
```

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| spuCode | String | 是 | SPU编码 |
| name | String | 是 | 商品名称 |
| categoryId | Long | 是 | 分类ID |
| brand | String | 否 | 品牌 |
| mainImage | String | 否 | 主图URL |
| description | String | 否 | 商品描述 |
| price | BigDecimal | 否 | 参考售价 |
| status | Integer | 否 | 状态：0-禁用 1-启用，默认1 |

**响应示例：**

```json
{
  "code": 200,
  "message": "success",
  "data": 1
}
```

### 2.4 修改商品

**PUT** `/api/products`

**请求参数：**

```json
{
  "id": 1,
  "spuCode": "SPU001",
  "name": "红色花朵鞋-新款",
  "categoryId": 1,
  "brand": "Nike",
  "price": 399.00,
  "status": 1
}
```

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| id | Long | 是 | 商品ID |
| 其他字段同新增 | | | |

### 2.5 删除商品

**DELETE** `/api/products/{id}`

**路径参数：**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| id | Long | 是 | 商品ID |

---

### 2.6 SKU 列表

**GET** `/api/skus/product/{productId}`

**路径参数：**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| productId | Long | 是 | 商品ID |

### 2.7 新增 SKU

**POST** `/api/skus`

**请求参数：**

```json
{
  "productId": 1,
  "skuCode": "SKU001",
  "name": "红色花朵鞋-42码",
  "spec": "{\"颜色\":\"红\",\"尺码\":\"42\"}",
  "costPrice": 150.00,
  "salePrice": 299.00,
  "weight": 0.8,
  "status": 1
}
```

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| productId | Long | 是 | 商品ID |
| skuCode | String | 是 | SKU编码 |
| name | String | 是 | SKU名称 |
| spec | String | 否 | 规格属性（JSON） |
| costPrice | BigDecimal | 否 | 成本价 |
| salePrice | BigDecimal | 否 | 售价 |
| weight | BigDecimal | 否 | 重量（kg） |
| status | Integer | 否 | 状态，默认1 |

### 2.8 删除 SKU

**DELETE** `/api/skus/{id}`

---

### 2.9 分类树

**GET** `/api/categories/tree`

**响应示例：**

```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "id": 1,
      "name": "鞋子",
      "parentId": 0,
      "sortOrder": 1,
      "status": 1,
      "children": [
        {
          "id": 2,
          "name": "运动鞋",
          "parentId": 1,
          "sortOrder": 1,
          "status": 1,
          "children": []
        }
      ]
    }
  ]
}
```

### 2.10 新增分类

**POST** `/api/categories`

**请求参数：**

```json
{
  "name": "运动鞋",
  "parentId": 1,
  "sortOrder": 1,
  "status": 1
}
```

---

## 3. 仓库管理

### 3.1 仓库列表

**GET** `/api/warehouses`

**请求参数：**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| page | Integer | 否 | 页码 |
| size | Integer | 否 | 每页数量 |
| keyword | String | 否 | 关键词（名称/编码） |
| status | Integer | 否 | 状态 |

**响应示例：**

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "total": 5,
    "list": [
      {
        "id": 1,
        "code": "WH001",
        "name": "主仓库",
        "address": "广东省广州市...",
        "contact": "张三",
        "phone": "13800138000",
        "status": 1,
        "createdAt": "2026-06-08T10:00:00"
      }
    ]
  }
}
```

### 3.2 新增仓库

**POST** `/api/warehouses`

**请求参数：**

```json
{
  "code": "WH001",
  "name": "主仓库",
  "address": "广东省广州市...",
  "contact": "张三",
  "phone": "13800138000",
  "status": 1
}
```

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| code | String | 是 | 仓库编码 |
| name | String | 是 | 仓库名称 |
| address | String | 否 | 地址 |
| contact | String | 否 | 联系人 |
| phone | String | 否 | 联系电话 |
| status | Integer | 否 | 状态，默认1 |

### 3.3 库区列表

**GET** `/api/warehouse-areas/warehouse/{warehouseId}`

### 3.4 新增库区

**POST** `/api/warehouse-areas`

**请求参数：**

```json
{
  "warehouseId": 1,
  "code": "A",
  "name": "A区",
  "type": 1,
  "status": 1
}
```

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| warehouseId | Long | 是 | 仓库ID |
| code | String | 是 | 库区编码 |
| name | String | 是 | 库区名称 |
| type | Integer | 否 | 类型：1-普通区 2-退货区 3-次品区 |
| status | Integer | 否 | 状态 |

### 3.5 货架列表

**GET** `/api/warehouse-shelves/area/{areaId}`

### 3.6 新增货架

**POST** `/api/warehouse-shelves`

**请求参数：**

```json
{
  "areaId": 1,
  "code": "A01",
  "name": "A01货架",
  "status": 1
}
```

### 3.7 库位列表

**GET** `/api/warehouse-locations/shelf/{shelfId}`

### 3.8 新增库位

**POST** `/api/warehouse-locations`

**请求参数：**

```json
{
  "shelfId": 1,
  "code": "A01-01",
  "name": "A01-01库位",
  "type": 1,
  "capacity": 100,
  "status": 1
}
```

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| shelfId | Long | 是 | 货架ID |
| code | String | 是 | 库位编码 |
| name | String | 是 | 库位名称 |
| type | Integer | 否 | 类型：1-普通 2-退货 3-次品 |
| capacity | Integer | 否 | 容量上限 |
| status | Integer | 否 | 状态 |

---

## 4. 库存管理

### 4.1 库存查询

**GET** `/api/stocks/query`

**请求参数：**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| page | Integer | 否 | 页码 |
| size | Integer | 否 | 每页数量 |
| skuId | Long | 否 | SKU ID |
| skuCode | String | 否 | SKU编码 |
| warehouseId | Long | 否 | 仓库ID |
| locationId | Long | 否 | 库位ID |

**响应示例：**

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "total": 100,
    "list": [
      {
        "id": 1,
        "skuId": 1,
        "skuCode": "SKU001",
        "skuName": "红色花朵鞋-42码",
        "warehouseId": 1,
        "warehouseName": "主仓库",
        "locationId": 1,
        "locationCode": "A01-01",
        "quantity": 50,
        "lockedQty": 5,
        "defectiveQty": 2,
        "createdAt": "2026-06-08T10:00:00",
        "updatedAt": "2026-06-08T10:00:00"
      }
    ]
  }
}
```

### 4.2 库存调整

**POST** `/api/stocks/adjust`

**请求参数：**

```json
{
  "skuId": 1,
  "warehouseId": 1,
  "locationId": 1,
  "quantity": 10,
  "remark": "采购入库"
}
```

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| skuId | Long | 是 | SKU ID |
| warehouseId | Long | 是 | 仓库ID |
| locationId | Long | 是 | 库位ID |
| quantity | Integer | 是 | 调整数量（正数增加，负数减少） |
| remark | String | 否 | 备注 |

### 4.3 库存流水

**GET** `/api/stock-logs`

**请求参数：**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| page | Integer | 否 | 页码 |
| size | Integer | 否 | 每页数量 |
| bizType | String | 否 | 业务类型：INBOUND/OUTBOUND/RETURN/ADJUST |
| bizNo | String | 否 | 业务单号 |
| skuId | Long | 否 | SKU ID |
| warehouseId | Long | 否 | 仓库ID |

**响应示例：**

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "total": 200,
    "list": [
      {
        "id": 1,
        "bizType": "INBOUND",
        "bizNo": "PO202606080001",
        "skuId": 1,
        "skuCode": "SKU001",
        "warehouseId": 1,
        "warehouseName": "主仓库",
        "locationId": 1,
        "locationCode": "A01-01",
        "quantityBefore": 0,
        "quantityChange": 100,
        "quantityAfter": 100,
        "operatorId": 1,
        "operatorName": "管理员",
        "remark": "采购入库",
        "createdAt": "2026-06-08T10:00:00"
      }
    ]
  }
}
```

### 4.4 创建盘点单

**POST** `/api/stock-checks`

**请求参数：**

```json
{
  "warehouseId": 1,
  "remark": "月度盘点"
}
```

### 4.5 盘点单详情

**GET** `/api/stock-checks/{id}`

### 4.6 提交盘点结果

**POST** `/api/stock-checks/submit`

**请求参数：**

```json
{
  "checkId": 1,
  "items": [
    {
      "itemId": 1,
      "actualQty": 48
    },
    {
      "itemId": 2,
      "actualQty": 100
    }
  ]
}
```

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| checkId | Long | 是 | 盘点单ID |
| items | Array | 是 | 盘点明细列表 |
| items[].itemId | Long | 是 | 明细ID |
| items[].actualQty | Integer | 是 | 实际盘点数量 |

---

## 5. 订单管理

### 5.1 订单列表

**GET** `/api/orders`

**请求参数：**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| page | Integer | 否 | 页码 |
| size | Integer | 否 | 每页数量 |
| orderNo | String | 否 | 订单号 |
| platformOrderNo | String | 否 | 平台订单号 |
| receiverName | String | 否 | 收件人姓名 |
| receiverPhone | String | 否 | 收件人电话 |
| orderStatus | String | 否 | 订单状态 |
| warehouseId | Long | 否 | 仓库ID |

**响应示例：**

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "total": 1000,
    "list": [
      {
        "id": 1,
        "orderNo": "SO202606080001",
        "platformOrderNo": "TB202606080001",
        "warehouseId": 1,
        "warehouseName": "主仓库",
        "receiverName": "张三",
        "receiverPhone": "13800138000",
        "receiverAddress": "广东省广州市天河区...",
        "orderStatus": "WAIT_OUTBOUND",
        "totalAmount": 299.00,
        "remark": "",
        "createdAt": "2026-06-08T10:00:00"
      }
    ]
  }
}
```

### 5.2 订单详情

**GET** `/api/orders/{id}`

**响应示例：**

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 1,
    "orderNo": "SO202606080001",
    "platformOrderNo": "TB202606080001",
    "warehouseId": 1,
    "receiverName": "张三",
    "receiverPhone": "13800138000",
    "receiverAddress": "广东省广州市天河区...",
    "orderStatus": "WAIT_OUTBOUND",
    "totalAmount": 299.00,
    "items": [
      {
        "id": 1,
        "orderId": 1,
        "skuId": 1,
        "skuCode": "SKU001",
        "skuName": "红色花朵鞋-42码",
        "quantity": 1,
        "unitPrice": 299.00,
        "totalPrice": 299.00
      }
    ],
    "createdAt": "2026-06-08T10:00:00"
  }
}
```

### 5.3 导入订单

**POST** `/api/orders/import`

**请求参数：**

```json
{
  "platformOrderNo": "TB202606080001",
  "warehouseId": 1,
  "receiverName": "张三",
  "receiverPhone": "13800138000",
  "receiverAddress": "广东省广州市天河区...",
  "remark": "",
  "items": [
    {
      "skuId": 1,
      "skuCode": "SKU001",
      "skuName": "红色花朵鞋-42码",
      "quantity": 1,
      "unitPrice": 299.00
    }
  ]
}
```

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| platformOrderNo | String | 否 | 平台订单号 |
| warehouseId | Long | 否 | 仓库ID |
| receiverName | String | 是 | 收件人姓名 |
| receiverPhone | String | 是 | 收件人电话 |
| receiverAddress | String | 是 | 收件人地址 |
| remark | String | 否 | 备注 |
| items | Array | 是 | 订单明细 |
| items[].skuId | Long | 否 | SKU ID |
| items[].skuCode | String | 是 | SKU编码 |
| items[].skuName | String | 是 | SKU名称 |
| items[].quantity | Integer | 是 | 数量 |
| items[].unitPrice | BigDecimal | 是 | 单价 |

### 5.4 更新订单状态

**PUT** `/api/orders/{id}/status`

**请求参数：**

```json
{
  "targetStatus": "WAIT_OUTBOUND"
}
```

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| targetStatus | String | 是 | 目标状态 |

**订单状态流转：**

```
WAIT_PAY → WAIT_OUTBOUND → OUTBOUNDING → SHIPPED → FINISHED
    ↓            ↓
CANCELLED    CANCELLED

SHIPPED → RETURNING → RETURNED
```

### 5.5 订单搜索

**GET** `/api/orders/search`

**请求参数：**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| page | Integer | 否 | 页码 |
| size | Integer | 否 | 每页数量 |
| keyword | String | 否 | 搜索关键词 |
| orderStatus | String | 否 | 订单状态 |
| warehouseId | Long | 否 | 仓库ID |
| startTime | String | 否 | 开始时间 |
| endTime | String | 否 | 结束时间 |

---

## 6. 出库流程

### 6.1 出库单列表

**GET** `/api/outbound/list`

**请求参数：**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| page | Integer | 否 | 页码 |
| size | Integer | 否 | 每页数量 |
| outboundNo | String | 否 | 出库单号 |
| orderNo | String | 否 | 订单号 |
| status | String | 否 | 状态 |
| warehouseId | Long | 否 | 仓库ID |

**响应示例：**

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "total": 50,
    "list": [
      {
        "id": 1,
        "outboundNo": "OB202606080001",
        "orderId": 1,
        "orderNo": "SO202606080001",
        "warehouseId": 1,
        "warehouseName": "主仓库",
        "status": "WAIT_PICKING",
        "pickerId": null,
        "pickerName": null,
        "remark": "",
        "createdAt": "2026-06-08T10:00:00"
      }
    ]
  }
}
```

### 6.2 出库单详情

**GET** `/api/outbound/{id}`

**响应示例：**

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 1,
    "outboundNo": "OB202606080001",
    "orderId": 1,
    "orderNo": "SO202606080001",
    "warehouseId": 1,
    "status": "WAIT_PICKING",
    "items": [
      {
        "id": 1,
        "outboundId": 1,
        "skuId": 1,
        "skuCode": "SKU001",
        "skuName": "红色花朵鞋-42码",
        "quantity": 1,
        "pickedQty": 0,
        "locationId": null,
        "locationCode": null,
        "scanned": 0
      }
    ],
    "createdAt": "2026-06-08T10:00:00"
  }
}
```

### 6.3 创建出库单

**POST** `/api/outbound/create`

**请求参数：**

```json
{
  "orderId": 1,
  "remark": ""
}
```

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| orderId | Long | 是 | 订单ID |
| remark | String | 否 | 备注 |

### 6.4 扫码核对

**POST** `/api/outbound/scan`

**请求参数：**

```json
{
  "outboundId": 1,
  "scanCode": "SKU001",
  "locationId": 1
}
```

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| outboundId | Long | 是 | 出库单ID |
| scanCode | String | 是 | SKU编码或条码 |
| locationId | Long | 是 | 拣货库位ID |

### 6.5 确认出库

**POST** `/api/outbound/confirm`

**请求参数：**

```json
{
  "outboundId": 1
}
```

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| outboundId | Long | 是 | 出库单ID |

**业务逻辑：**

1. 检查所有明细是否已扫码
2. 扣减库存（防负数）
3. 写库存流水
4. 更新出库单状态为 SHIPPED
5. 更新订单状态为 SHIPPED

---

## 7. 退货流程

### 7.1 退货单列表

**GET** `/api/returns/list`

**请求参数：**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| page | Integer | 否 | 页码 |
| size | Integer | 否 | 每页数量 |
| returnNo | String | 否 | 退货单号 |
| orderNo | String | 否 | 订单号 |
| status | String | 否 | 状态 |
| warehouseId | Long | 否 | 仓库ID |

**响应示例：**

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "total": 20,
    "list": [
      {
        "id": 1,
        "returnNo": "RT202606080001",
        "orderId": 1,
        "orderNo": "SO202606080001",
        "warehouseId": 1,
        "status": "PENDING_CHECK",
        "reason": "尺码不合适",
        "remark": "",
        "createdAt": "2026-06-08T10:00:00"
      }
    ]
  }
}
```

### 7.2 退货单详情

**GET** `/api/returns/{id}`

### 7.3 创建退货单

**POST** `/api/returns/create`

**请求参数：**

```json
{
  "orderId": 1,
  "reason": "尺码不合适",
  "remark": "",
  "items": [
    {
      "skuId": 1,
      "quantity": 1
    }
  ]
}
```

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| orderId | Long | 是 | 订单ID |
| reason | String | 是 | 退货原因 |
| remark | String | 否 | 备注 |
| items | Array | 否 | 退货明细 |
| items[].skuId | Long | 是 | SKU ID |
| items[].quantity | Integer | 是 | 退货数量 |

### 7.4 退货质检

**POST** `/api/returns/check`

**请求参数：**

```json
{
  "returnId": 1,
  "items": [
    {
      "itemId": 1,
      "qualityStatus": "SELLABLE",
      "locationId": 1
    }
  ]
}
```

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| returnId | Long | 是 | 退货单ID |
| items | Array | 是 | 质检明细 |
| items[].itemId | Long | 是 | 明细ID |
| items[].qualityStatus | String | 是 | 质检状态：SELLABLE/DEFECTIVE/SCRAPPED |
| items[].locationId | Long | 是 | 入库库位ID |

### 7.5 确认退货入库

**POST** `/api/returns/confirm`

**请求参数：**

```json
{
  "returnId": 1
}
```

---

## 8. 文件上传

### 8.1 上传文件

**POST** `/api/files/upload`

**Content-Type:** `multipart/form-data`

**请求参数：**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| file | File | 是 | 文件 |
| bizType | String | 否 | 业务类型 |
| bizId | Long | 否 | 业务ID |

**响应示例：**

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 1,
    "fileName": "product.jpg",
    "filePath": "/uploads/2026/06/08/uuid.jpg",
    "fileSize": 102400,
    "fileType": "image/jpeg",
    "url": "http://localhost:8080/uploads/2026/06/08/uuid.jpg",
    "createdAt": "2026-06-08T10:00:00"
  }
}
```

### 8.2 获取文件信息

**GET** `/api/files/{id}`

### 8.3 删除文件

**DELETE** `/api/files/{id}`

---

## 9. 操作日志

### 9.1 日志列表

**GET** `/api/operation-logs`

**请求参数：**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| page | Integer | 否 | 页码 |
| size | Integer | 否 | 每页数量 |
| userId | Long | 否 | 操作人ID |
| operation | String | 否 | 操作类型 |
| status | Integer | 否 | 状态：0-失败 1-成功 |

---

## 10. 报表统计

### 10.1 首页仪表盘

**GET** `/api/reports/dashboard`

**响应示例：**

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "todayOrderCount": 50,
    "pendingOutboundCount": 10,
    "todayOutboundCount": 30,
    "todayReturnCount": 5,
    "stockAlertCount": 8,
    "orderTrend": [
      {"date": "2026-06-02", "count": 45},
      {"date": "2026-06-03", "count": 52},
      {"date": "2026-06-04", "count": 48},
      {"date": "2026-06-05", "count": 60},
      {"date": "2026-06-06", "count": 55},
      {"date": "2026-06-07", "count": 42},
      {"date": "2026-06-08", "count": 50}
    ],
    "orderStatusDistribution": [
      {"status": "WAIT_PAY", "statusName": "待付款", "count": 5},
      {"status": "WAIT_OUTBOUND", "statusName": "待出库", "count": 10},
      {"status": "OUTBOUNDING", "statusName": "出库中", "count": 8},
      {"status": "SHIPPED", "statusName": "已发货", "count": 15},
      {"status": "FINISHED", "statusName": "已完成", "count": 100},
      {"status": "CANCELLED", "statusName": "已取消", "count": 2}
    ]
  }
}
```

### 10.2 库存报表

**GET** `/api/reports/stock`

**响应示例：**

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "totalSkuCount": 150,
    "totalQuantity": 5000,
    "totalLockedQty": 200,
    "totalDefectiveQty": 50,
    "warehouseStocks": [
      {
        "warehouseId": 1,
        "warehouseName": "主仓库",
        "quantity": 3000,
        "lockedQty": 100,
        "defectiveQty": 30
      },
      {
        "warehouseId": 2,
        "warehouseName": "分仓库",
        "quantity": 2000,
        "lockedQty": 100,
        "defectiveQty": 20
      }
    ]
  }
}
```

### 10.3 出库报表

**GET** `/api/reports/outbound`

**响应示例：**

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "todayOutboundCount": 30,
    "monthOutboundCount": 500,
    "pendingPickingCount": 10,
    "outboundTrend": [
      {"date": "2026-06-02", "count": 25},
      {"date": "2026-06-03", "count": 30},
      {"date": "2026-06-04", "count": 28},
      {"date": "2026-06-05", "count": 35},
      {"date": "2026-06-06", "count": 32},
      {"date": "2026-06-07", "count": 22},
      {"date": "2026-06-08", "count": 30}
    ]
  }
}
```

---

## 11. 健康检查

### 11.1 心跳接口

**GET** `/api/health`

**响应：**

```
helloWms
```

---

## 错误码

| 错误码 | 说明 |
|--------|------|
| 200 | 操作成功 |
| 400 | 请求参数错误 |
| 401 | 未登录或登录已过期 |
| 403 | 无权限访问 |
| 404 | 资源不存在 |
| 500 | 服务器内部错误 |
| 1001 | 用户不存在 |
| 1002 | 密码错误 |
| 1003 | 用户已禁用 |
| 1004 | 用户已存在 |
| 2001 | 商品不存在 |
| 2002 | SKU不存在 |
| 2003 | 条码已存在 |
| 3001 | 仓库不存在 |
| 3002 | 库位不存在 |
| 4001 | 库存不足 |
| 5001 | 订单不存在 |
| 5002 | 订单状态不正确 |
| 6001 | 出库单不存在 |
| 6002 | 出库单状态不正确 |
| 7001 | 退货单不存在 |
| 7002 | 退货单状态不正确 |

---

*文档生成时间：2026-06-09*
