#!/bin/bash
# ============================================================
# WMS 本地打包脚本 - 生成 wms-deploy.tar.gz 部署包
# 在项目根目录（/root/wms）执行：bash docker/build-deploy.sh
# ============================================================

set -e

RED='\033[0;31m'
GREEN='\033[0;32m'
NC='\033[0m'

log() { echo -e "${GREEN}[INFO]${NC} $1"; }

# 项目根目录 = 当前执行命令的目录
PROJECT_ROOT="$(pwd)"
BUILD_DIR="${PROJECT_ROOT}/.deploy-build"
OUTPUT="${PROJECT_ROOT}/wms-deploy.tar.gz"

# 检查目录结构
if [ ! -d "${PROJECT_ROOT}/wms-backend" ] || [ ! -d "${PROJECT_ROOT}/wsm-web" ]; then
    echo -e "${RED}[ERROR]${NC} 请在项目根目录执行（包含 wms-backend/ 和 wsm-web/ 的目录）"
    echo "用法: cd /root/wms && bash docker/build-deploy.sh"
    exit 1
fi

rm -rf "${BUILD_DIR}"
mkdir -p "${BUILD_DIR}"/{wms-backend,wsm-web,docker}

# ============================================================
# 1. 构建后端 JAR
# ============================================================
log "构建后端 JAR..."
cd "${PROJECT_ROOT}/wms-backend"

if [ -f "mvnw" ]; then
    chmod +x mvnw
    ./mvnw package -DskipTests -Dmaven.test.skip=true -q
else
    mvn package -DskipTests -Dmaven.test.skip=true -q
fi

cp target/*.jar "${BUILD_DIR}/wms-backend/app.jar"
cp -r sql "${BUILD_DIR}/wms-backend/"

# 后端 Dockerfile（直接用 JAR，不需要编译阶段）
cat > "${BUILD_DIR}/wms-backend/Dockerfile" <<'DOCKERFILE'
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY app.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
DOCKERFILE

log "后端构建完成"

# ============================================================
# 2. 构建前端
# ============================================================
log "构建前端..."
cd "${PROJECT_ROOT}/wsm-web"

if [ -f "package-lock.json" ]; then
    npm ci --registry https://registry.npmmirror.com
else
    npm install --registry https://registry.npmmirror.com
fi
npm run build

cp -r dist "${BUILD_DIR}/wsm-web/"
cp "${PROJECT_ROOT}/wsm-web/nginx.conf" "${BUILD_DIR}/wsm-web/"

# 前端 Dockerfile（直接用 dist，不需要编译阶段）
cat > "${BUILD_DIR}/wsm-web/Dockerfile" <<'DOCKERFILE'
FROM nginx:alpine
COPY dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
DOCKERFILE

log "前端构建完成"

# ============================================================
# 3. 复制配置文件
# ============================================================
log "复制配置文件..."
cp "${PROJECT_ROOT}/docker/init.sql" "${BUILD_DIR}/docker/"

# docker-compose.yml
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
    build:
      context: ./wms-backend
      dockerfile: Dockerfile
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
      FILE_UPLOAD_PATH: /app/uploads
      IMAGE_STORAGE_PATH: /app/uploads/images
    volumes:
      - uploads_data:/app/uploads

  wms-frontend:
    build:
      context: ./wsm-web
      dockerfile: Dockerfile
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
