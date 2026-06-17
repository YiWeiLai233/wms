#!/bin/bash
# ============================================================
# WMS 系统手动部署脚本（不使用 Docker）
# 用法: sudo bash install-manual.sh
# 前提: 将 wms-backend.jar 和 wsm-web/dist 放在同目录下
# ============================================================

set -e

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

log()   { echo -e "${GREEN}[INFO]${NC} $1"; }
warn()  { echo -e "${YELLOW}[WARN]${NC} $1"; }
error() { echo -e "${RED}[ERROR]${NC} $1"; exit 1; }

# ============================================================
# 0. 检查前提条件
# ============================================================
if [ "$(id -u)" -ne 0 ]; then
    error "请使用 root 用户执行: sudo bash install-manual.sh"
fi

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
JAR_FILE="${SCRIPT_DIR}/wms-backend.jar"
DIST_DIR="${SCRIPT_DIR}/dist"

if [ ! -f "${JAR_FILE}" ]; then
    error "未找到 wms-backend.jar，请将其放在脚本同目录下"
fi

if [ ! -d "${DIST_DIR}" ]; then
    error "未找到 dist 目录，请将前端构建产物放在脚本同目录下的 dist/ 目录"
fi

# ============================================================
# 1. 检查 Java 环境
# ============================================================
check_java() {
    if ! command -v java &>/dev/null; then
        error "未检测到 Java，请先安装 Java 21: https://adoptium.net/"
    fi

    JAVA_VERSION=$(java -version 2>&1 | head -1 | cut -d'"' -f2 | cut -d'.' -f1)
    if [ "$JAVA_VERSION" -lt 17 ]; then
        error "Java 版本过低，需要 Java 17+，当前: $(java -version 2>&1 | head -1)"
    fi

    log "Java 环境正常: $(java -version 2>&1 | head -1)"
}

check_java

# ============================================================
# 2. 安装 MySQL
# ============================================================
install_mysql() {
    if command -v mysql &>/dev/null; then
        log "MySQL 已安装: $(mysql --version)"
        return
    fi

    log "安装 MySQL 8.0..."

    # 添加 MySQL 官方源
    rpm -Uvh https://dev.mysql.com/get/mysql80-community-release-el7-11.noarch.rpm 2>/dev/null || true
    yum install -y mysql-server

    systemctl start mysqld
    systemctl enable mysqld

    # 获取临时密码
    TEMP_PWD=$(grep 'temporary password' /var/log/mysqld.log | awk '{print $NF}' | tail -1)
    log "MySQL 临时密码: ${TEMP_PWD}"
    warn "请在部署完成后修改 MySQL root 密码"
}

install_mysql

# ============================================================
# 3. 安装 Redis
# ============================================================
install_redis() {
    if command -v redis-server &>/dev/null; then
        log "Redis 已安装: $(redis-server --version)"
        return
    fi

    log "安装 Redis..."
    yum install -y epel-release
    yum install -y redis

    systemctl start redis
    systemctl enable redis
    log "Redis 安装完成"
}

install_redis

# ============================================================
# 4. 安装 Nginx
# ============================================================
install_nginx() {
    if command -v nginx &>/dev/null; then
        log "Nginx 已安装: $(nginx -v 2>&1)"
        return
    fi

    log "安装 Nginx..."
    yum install -y nginx
    systemctl start nginx
    systemctl enable nginx
    log "Nginx 安装完成"
}

install_nginx

# ============================================================
# 5. 部署应用
# ============================================================
INSTALL_DIR="/opt/wms"
log "部署应用到 ${INSTALL_DIR}..."

# 创建目录
mkdir -p "${INSTALL_DIR}"/{logs,uploads,backups}

# 复制后端
cp "${JAR_FILE}" "${INSTALL_DIR}/wms-backend.jar"

# 复制前端
rm -rf /usr/share/nginx/html/*
cp -r "${DIST_DIR}"/* /usr/share/nginx/html/

# ============================================================
# 6. 配置 Nginx
# ============================================================
log "配置 Nginx..."

cat > /etc/nginx/conf.d/wms.conf <<'EOF'
server {
    listen 80;
    server_name _;

    # 前端
    root /usr/share/nginx/html;
    index index.html;

    # API 代理
    location /api/ {
        proxy_pass http://127.0.0.1:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_connect_timeout 300;
        proxy_read_timeout 300;
    }

    # 图片代理
    location /images/ {
        proxy_pass http://127.0.0.1:8080;
    }

    # Vue Router history mode
    location / {
        try_files $uri $uri/ /index.html;
    }
}
EOF

nginx -t && systemctl reload nginx
log "Nginx 配置完成"

# ============================================================
# 7. 创建 systemd 服务
# ============================================================
log "创建后端服务..."

# 生成环境配置
MYSQL_PWD=$(openssl rand -base64 16 | tr -dc 'a-zA-Z0-9' | head -c 16)
JWT_SECRET=$(openssl rand -base64 32 | tr -dc 'a-zA-Z0-9' | head -c 32)

cat > "${INSTALL_DIR}/.env" <<EOF
# 数据库配置
SPRING_DATASOURCE_URL=jdbc:mysql://127.0.0.1:3306/wms?useSSL=false&serverTimezone=Asia/Shanghai&characterEncoding=utf8&allowPublicKeyRetrieval=true
SPRING_DATASOURCE_USERNAME=root
SPRING_DATASOURCE_PASSWORD=Wms@${MYSQL_PWD}

# Redis 配置
SPRING_DATA_REDIS_HOST=127.0.0.1
SPRING_DATA_REDIS_PORT=6379
SPRING_DATA_REDIS_DATABASE=0
SPRING_DATA_REDIS_PASSWORD=

# JWT 配置
JWT_SECRET=Wms${JWT_SECRET}2026
JWT_EXPIRATION=86400000

# 文件上传
FILE_UPLOAD_PATH=${INSTALL_DIR}/uploads
FILE_BASE_URL=http://localhost
IMAGE_STORAGE_PATH=${INSTALL_DIR}/uploads/images
IMAGE_BASE_URL=http://localhost/images

# 日志
LOG_LEVEL=info
EOF
chmod 600 "${INSTALL_DIR}/.env"

# 创建 systemd 服务
cat > /etc/systemd/system/wms.service <<EOF
[Unit]
Description=WMS Backend Service
After=mysql.service redis.service
Requires=mysql.service redis.service

[Service]
Type=simple
User=root
WorkingDirectory=${INSTALL_DIR}
EnvironmentFile=${INSTALL_DIR}/.env
ExecStart=/usr/bin/java -Xms256m -Xmx512m -jar ${INSTALL_DIR}/wms-backend.jar --spring.profiles.active=prod
Restart=always
RestartSec=10

[Install]
WantedBy=multi-user.target
EOF

systemctl daemon-reload
systemctl enable wms

# ============================================================
# 8. 初始化数据库
# ============================================================
log "初始化数据库..."

# 设置 MySQL root 密码
mysql -u root -e "ALTER USER 'root'@'localhost' IDENTIFIED BY 'Wms@${MYSQL_PWD}';" 2>/dev/null || true
mysql -u root -p"Wms@${MYSQL_PWD}" -e "CREATE DATABASE IF NOT EXISTS wms DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;" 2>/dev/null || true

# 导入 SQL
if [ -f "${SCRIPT_DIR}/init.sql" ]; then
    mysql -u root -p"Wms@${MYSQL_PWD}" wms < "${SCRIPT_DIR}/init.sql" 2>/dev/null || warn "SQL 导入可能需要手动执行"
fi

# ============================================================
# 9. 启动服务
# ============================================================
log "启动后端服务..."
systemctl start wms

# 等待启动
log "等待服务启动..."
for i in $(seq 1 30); do
    if curl -s http://localhost:8080/api/health >/dev/null 2>&1; then
        log "后端服务已启动"
        break
    fi
    [ "$i" -eq 30 ] && warn "服务启动超时，请检查日志: journalctl -u wms -f"
    sleep 2
done

# ============================================================
# 10. 配置防火墙
# ============================================================
if command -v firewall-cmd &>/dev/null; then
    log "配置防火墙..."
    firewall-cmd --permanent --add-port=80/tcp 2>/dev/null || true
    firewall-cmd --reload 2>/dev/null || true
fi

# ============================================================
# 11. 完成
# ============================================================
SERVER_IP=$(hostname -I | awk '{print $1}')

cat <<INFO

============================================================
  WMS 系统部署完成！
============================================================

  前端访问:    http://${SERVER_IP}
  后端 API:    http://${SERVER_IP}:8080

  默认账号:    admin
  默认密码:    123456
  MySQL 密码:  Wms@${MYSQL_PWD}

  安装目录:    ${INSTALL_DIR}
  环境配置:    ${INSTALL_DIR}/.env

  常用命令:
    查看状态:  systemctl status wms
    查看日志:  journalctl -u wms -f
    重启服务:  systemctl restart wms
    停止服务:  systemctl stop wms

============================================================
INFO
