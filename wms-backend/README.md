# 易仓 WMS 后端

> 局域网多端仓库管理系统后端

## 项目简介

易仓WMS是一个局域网部署的仓库管理系统，提供RESTful API，支持商品管理、仓库管理、库存管理、订单管理、出库流程、退货处理等核心业务功能。

## 技术栈

| 组件 | 技术 | 版本 |
|------|------|------|
| 框架 | Spring Boot | 4.0.6 |
| ORM | MyBatis | 4.0.1 |
| 数据库 | MySQL | 8.x |
| JDK | Java | 17 |
| 认证 | Spring Security + JWT | - |
| 工具库 | Lombok | - |
| 构建 | Maven | - |

## 快速开始

### 1. 环境准备

- JDK 17+
- MySQL 8.x
- Maven 3.6+

### 2. 数据库初始化

```sql
-- 执行建表脚本
source sql/schema.sql
```

### 3. 修改配置

编辑 `src/main/resources/application.properties`：

```properties
# 数据库配置
spring.datasource.url=jdbc:mysql://localhost:3306/wms?useSSL=false&serverTimezone=Asia/Shanghai
spring.datasource.username=root
spring.datasource.password=your_password

# JWT密钥（建议修改）
jwt.secret=your_jwt_secret_key_here

# 文件上传路径
file.upload-dir=./uploads
```

### 4. 启动项目

```bash
# 使用Maven启动
./mvnw spring-boot:run

# 或打包后运行
./mvnw package
java -jar target/wms-0.0.1-SNAPSHOT.jar
```

### 5. 访问接口

- 服务地址：`http://localhost:8080`
- API文档：`http://localhost:8080/doc.html`（Knife4j）
- 健康检查：`http://localhost:8080/api/health`

### 6. 默认账号

```
用户名：admin
密码：123456
```

## 项目结构

```
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
 ├── search              搜索模块
 ├── file                文件上传模块
 ├── log                 操作日志模块
 ├── report              报表统计模块
 └── ocr                 OCR识别模块
```

## API接口

### 认证相关

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | /api/auth/login | 用户登录 |
| GET | /api/auth/profile | 获取当前用户信息 |

### 用户管理

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /api/users | 用户列表 |
| GET | /api/users/{id} | 用户详情 |
| POST | /api/users | 新增用户 |
| PUT | /api/users | 修改用户 |
| DELETE | /api/users/{id} | 删除用户 |
| PUT | /api/users/{id}/reset-password | 重置密码 |
| GET | /api/roles | 角色列表 |

### 商品管理

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /api/products | 商品列表 |
| GET | /api/products/{id} | 商品详情 |
| POST | /api/products | 新增商品 |
| PUT | /api/products | 修改商品 |
| DELETE | /api/products/{id} | 删除商品 |
| GET | /api/skus/product/{productId} | SKU列表 |
| POST | /api/skus | 新增SKU |
| GET | /api/categories/tree | 分类树 |

### 仓库管理

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /api/warehouses | 仓库列表 |
| POST | /api/warehouses | 新增仓库 |
| GET | /api/warehouse-areas/warehouse/{id} | 库区列表 |
| GET | /api/warehouse-shelves/area/{id} | 货架列表 |
| GET | /api/warehouse-locations/shelf/{id} | 库位列表 |

### 库存管理

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /api/stocks/query | 库存查询 |
| POST | /api/stocks/adjust | 库存调整 |
| GET | /api/stock-logs | 库存流水 |
| POST | /api/stock-checks | 创建盘点单 |
| POST | /api/stock-checks/submit | 提交盘点结果 |

### 订单管理

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /api/orders | 订单列表 |
| GET | /api/orders/{id} | 订单详情 |
| POST | /api/orders/import | 导入订单 |
| PUT | /api/orders/{id}/status | 更新订单状态 |
| GET | /api/orders/search | 订单搜索 |

### 出库流程

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /api/outbound/list | 出库单列表 |
| POST | /api/outbound/create | 创建出库单 |
| POST | /api/outbound/scan | 扫码核对 |
| POST | /api/outbound/confirm | 确认出库 |

### 退货流程

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /api/returns/list | 退货单列表 |
| POST | /api/returns/create | 创建退货单 |
| POST | /api/returns/check | 退货质检 |
| POST | /api/returns/confirm | 确认退货入库 |

### 文件上传

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | /api/files/upload | 上传文件 |
| GET | /api/files/{id} | 获取文件信息 |

### 报表统计

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /api/reports/dashboard | 首页仪表盘 |
| GET | /api/reports/stock | 库存报表 |
| GET | /api/reports/outbound | 出库报表 |

### OCR识别

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | /api/ocr/express | 快递单识别 |
| POST | /api/ocr/express/upload | 快递单识别（文件） |
| POST | /api/ocr/general | 通用文字识别 |
| POST | /api/ocr/general/upload | 通用文字识别（文件） |

### 操作日志

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /api/operation-logs | 操作日志列表 |

## 核心业务规则

### 库存扣减

```sql
-- 必须在事务内执行
-- 扣减SQL必须加防负数条件
UPDATE stock SET quantity = quantity - #{num}
WHERE sku_id = #{skuId} AND quantity >= #{num};
-- 影响行数为0时，抛出库存不足异常
```

### 订单状态流转

```
WAIT_PAY → WAIT_OUTBOUND → OUTBOUNDING → SHIPPED → FINISHED
    ↓            ↓
CANCELLED    CANCELLED
```

### 出库流程

```
创建出库单 → 扫码核对 → 确认出库（扣减库存+写流水）
```

### 退货流程

```
创建退货单 → 退货质检 → 确认入库（增加库存+写流水）
```

## OCR配置

### 百度OCR

1. 申请百度AI开放平台：https://ai.baidu.com/tech/ocr
2. 获取API Key和Secret Key
3. 修改配置：

```properties
ocr.baidu.enabled=true
ocr.baidu.api-key=your_api_key
ocr.baidu.secret-key=your_secret_key
```

### Tesseract本地OCR

1. 下载安装：https://github.com/tesseract-ocr/tesseract
2. 配置环境变量
3. 使用时指定engine参数：`engine=tesseract`

## 统一响应格式

```json
{
  "code": 200,
  "message": "success",
  "data": {}
}
```

### 分页响应

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

## 错误码

| 错误码 | 说明 |
|--------|------|
| 200 | 操作成功 |
| 400 | 请求参数错误 |
| 401 | 未登录或登录已过期 |
| 403 | 无权限访问 |
| 404 | 资源不存在 |
| 500 | 服务器内部错误 |

## 部署

### Docker部署

```dockerfile
FROM openjdk:17-jdk-slim
COPY target/wms-*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app.jar"]
```

### Docker Compose

```yaml
version: '3'
services:
  mysql:
    image: mysql:8.0
    environment:
      MYSQL_ROOT_PASSWORD: root123
      MYSQL_DATABASE: wms
    ports:
      - "3306:3306"
    volumes:
      - mysql_data:/var/lib/mysql
      - ./sql/schema.sql:/docker-entrypoint-initdb.d/init.sql

  wms:
    build: .
    ports:
      - "8080:8080"
    depends_on:
      - mysql
    environment:
      SPRING_DATASOURCE_URL: jdbc:mysql://mysql:3306/wms?useSSL=false&serverTimezone=Asia/Shanghai
      SPRING_DATASOURCE_USERNAME: root
      SPRING_DATASOURCE_PASSWORD: root123

volumes:
  mysql_data:
```

## 开发规范

- RESTful API设计
- 统一响应封装 `Result<T>`
- 全局异常处理
- Lombok减少样板代码
- MyBatis XML映射SQL
- JWT无状态认证

## 文档

- [API接口文档](docs/api-doc.md)
- [用户管理接口](docs/user-api.md)
- [框架设计文档](docs/backend-framework.md)

## 许可证

内部项目，仅供学习使用。
