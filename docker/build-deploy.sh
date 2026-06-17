#!/bin/bash
# ============================================================
# WMS 打包脚本 - 用 Docker 构建，服务器无需安装 Java/Node.js
# 在项目根目录执行：cd /root/wms && bash docker/build-deploy.sh
# ============================================================

set -e

RED='\033[0;31m'
GREEN='\033[0;32m'
NC='\033[0m'

log() { echo -e "${GREEN}[INFO]${NC} $1"; }

PROJECT_ROOT="$(pwd)"
BUILD_DIR="${PROJECT_ROOT}/.deploy-build"
OUTPUT="${PROJECT_ROOT}/wms-deploy.tar.gz"

if [ ! -d "${PROJECT_ROOT}/wms-backend" ] || [ ! -d "${PROJECT_ROOT}/wsm-web" ]; then
    echo -e "${RED}[ERROR]${NC} 请在项目根目录执行（包含 wms-backend/ 和 wsm-web/ 的目录）"
    echo "用法: cd /root/wms && bash docker/build-deploy.sh"
    exit 1
fi

rm -rf "${BUILD_DIR}"
mkdir -p "${BUILD_DIR}"/{wms-backend,wsm-web,docker}

# ============================================================
# 1. 用 Docker 构建后端 JAR
# ============================================================
log "用 Docker 构建后端 JAR（无需本地安装 Java）..."

cat > "${PROJECT_ROOT}/wms-backend/Dockerfile.build" <<'DOCKERFILE'
FROM maven:3.9-eclipse-temurin-17 AS builder
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -q
COPY src ./src
RUN mvn package -DskipTests -Dmaven.test.skip=true -q

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=builder /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar", "--spring.profiles.active=docker"]
DOCKERFILE

docker build -f "${PROJECT_ROOT}/wms-backend/Dockerfile.build" -t wms-backend:latest "${PROJECT_ROOT}/wms-backend"
docker save wms-backend:latest -o "${BUILD_DIR}/wms-backend.tar"
rm -f "${PROJECT_ROOT}/wms-backend/Dockerfile.build"

# 复制 SQL 初始化脚本
cp -r "${PROJECT_ROOT}/wms-backend/sql" "${BUILD_DIR}/wms-backend/"
cp "${PROJECT_ROOT}/docker/init.sql" "${BUILD_DIR}/docker/"

log "后端构建完成"

# ============================================================
# 2. 用 Docker 构建前端
# ============================================================
log "用 Docker 构建前端（无需本地安装 Node.js）..."

cat > "${PROJECT_ROOT}/wsm-web/Dockerfile.build" <<'DOCKERFILE'
FROM node:20-alpine AS builder
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci --registry https://registry.npmmirror.com
COPY . .
RUN npm run build

FROM nginx:alpine
COPY --from=builder /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
DOCKERFILE

docker build -f "${PROJECT_ROOT}/wsm-web/Dockerfile.build" -t wms-frontend:latest "${PROJECT_ROOT}/wsm-web"
docker save wms-frontend:latest -o "${BUILD_DIR}/wms-frontend.tar"
rm -f "${PROJECT_ROOT}/wsm-web/Dockerfile.build"

log "前端构建完成"

# ============================================================
# 3. 生成 docker-compose.yml（用预构建镜像）
# ============================================================
log "生成配置文件..."

cat > "${BUILD_DIR}/docker-compose.yml" <<'COMPOSE'
services:
  mysql:
    image: mysql:8.0
    container_name: wms-mysql
    restart: unless-stopped
    environment:
      MYSQL_ROOT_PASSWORD: ${MYSQL_ROOT_PASSWORD:-Wms@2026}
      MYSQL_DATABASE: wms
    ports:
      - "3306:3306"
    volumes:
      - mysql_data:/var/lib/mysql
      - ./docker/init.sql:/docker-entrypoint-initdb.d/01-init.sql
    command: --character-set-server=utf8mb4 --collation-server=utf8mb4_general_ci --default-authentication-plugin=mysql_native_password
    healthcheck:
      test: ["CMD", "mysqladmin", "ping", "-h", "localhost", "-uroot", "-p${MYSQL_ROOT_PASSWORD:-Wms@2026}"]
      interval: 10s
      timeout: 5s
      retries: 10

  redis:
    image: redis:7-alpine
    container_name: wms-redis
    restart: unless-stopped
    ports:
      - "6379:6379"
    volumes:
      - redis_data:/data
    healthcheck:
      test: ["CMD", "redis-cli", "ping"]
      interval: 10s
      timeout: 5s
      retries: 5

  wms-backend:
    image: wms-backend:latest
    container_name: wms-backend
    restart: unless-stopped
    depends_on:
      mysql:
        condition: service_healthy
      redis:
        condition: service_healthy
    ports:
      - "8080:8080"
    environment:
      MYSQL_URL: jdbc:mysql://mysql:3306/wms?useSSL=false&serverTimezone=Asia/Shanghai&characterEncoding=utf8&allowPublicKeyRetrieval=true
      MYSQL_USERNAME: root
      MYSQL_PASSWORD: ${MYSQL_ROOT_PASSWORD:-Wms@2026}
      REDIS_HOST: redis
      REDIS_PORT: 6379
      REDIS_DB: 0
      REDIS_PASSWORD: ${REDIS_PASSWORD:-}
      JWT_SECRET: ${JWT_SECRET:-WmsJwtSecret2026ChangeMeInProduction!}
      JWT_EXPIRATION: ${JWT_EXPIRATION:-86400000}
      FILE_UPLOAD_PATH: /app/uploads
      FILE_BASE_URL: ${FILE_BASE_URL:-http://localhost:8080}
      IMAGE_STORAGE_PATH: /app/uploads/images
      IMAGE_BASE_URL: ${IMAGE_BASE_URL:-http://localhost:8080/images}
      AI_SERVICE_BASE_URL: ${AI_SERVICE_BASE_URL:-http://localhost:8010}
      AI_SERVICE_TOKEN: ${AI_SERVICE_TOKEN:-ChangeMeAiServiceToken}
      SWAGGER_ENABLED: ${SWAGGER_ENABLED:-false}
      LOG_LEVEL: ${LOG_LEVEL:-info}
      PRIVACY_MASTER_KEY: ${PRIVACY_MASTER_KEY:-P3HBjvkFvIi7Q/MOLgHA32Fgx8eutYLL3sqMmTKKLE8=}
      PRIVACY_HASH_KEY: ${PRIVACY_HASH_KEY:-Td7H0vwQxi59YCQ8TpZXWpMiyS/lFadx2PMdQnLKuyU=}
    volumes:
      - uploads_data:/app/uploads

  wms-frontend:
    image: wms-frontend:latest
    container_name: wms-frontend
    restart: unless-stopped
    depends_on:
      - wms-backend
    ports:
      - "80:80"

volumes:
  mysql_data:
  redis_data:
  uploads_data:
COMPOSE

# 复制安装脚本
cp "${PROJECT_ROOT}/docker/install-standalone.sh" "${BUILD_DIR}/"

# ============================================================
# 4. 打包
# ============================================================
log "打包部署包..."
cd "${BUILD_DIR}"
tar -czf "${OUTPUT}" .

rm -rf "${BUILD_DIR}"

SIZE=$(du -h "${OUTPUT}" | cut -f1)
echo ""
echo "============================================================"
echo -e "${GREEN}  打包完成！${NC}"
echo "============================================================"
echo ""
echo "  部署包: ${OUTPUT} (${SIZE})"
echo ""
echo "  上传到服务器:"
echo "    scp ${OUTPUT} root@服务器IP:/opt/"
echo ""
echo "  在服务器上安装:"
echo "    cd /opt && tar -xzf wms-deploy.tar.gz && bash install-standalone.sh"
echo ""
echo "============================================================"
