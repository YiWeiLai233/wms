#!/bin/bash
# ============================================================
# WMS 系统 CentOS 一键安装脚本
# 用法: bash install.sh
# ============================================================

set -e

# 颜色
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

log()   { echo -e "${GREEN}[INFO]${NC} $1"; }
warn()  { echo -e "${YELLOW}[WARN]${NC} $1"; }
error() { echo -e "${RED}[ERROR]${NC} $1"; exit 1; }

# ============================================================
# 1. 检查系统
# ============================================================
log "检查系统环境..."

if [ "$(uname)" != "Linux" ]; then
    error "此脚本仅支持 Linux 系统"
fi

if [ "$(id -u)" -ne 0 ]; then
    error "请使用 root 用户执行此脚本: sudo bash install.sh"
fi

# ============================================================
# 2. 安装 Docker
# ============================================================
install_docker() {
    if command -v docker &>/dev/null; then
        log "Docker 已安装: $(docker --version)"
    else
        log "安装 Docker..."

        # 卸载旧版本
        yum remove -y docker docker-client docker-client-latest docker-common \
            docker-latest docker-latest-logrotate docker-logrotate docker-engine 2>/dev/null || true

        # 安装依赖
        yum install -y yum-utils device-mapper-persistent-data lvm2

        # 添加 Docker 官方源（国内用阿里云镜像）
        yum-config-manager --add-repo https://mirrors.aliyun.com/docker-ce/linux/centos/docker-ce.repo

        # 安装 Docker
        yum install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin

        # 启动 Docker
        systemctl start docker
        systemctl enable docker

        # 配置镜像加速
        mkdir -p /etc/docker
        cat > /etc/docker/daemon.json <<'EOF'
{
    "registry-mirrors": [
        "https://docker.1ms.run",
        "https://docker.xuanyuan.me"
    ],
    "log-driver": "json-file",
    "log-opts": {
        "max-size": "10m",
        "max-file": "3"
    }
}
EOF
        systemctl daemon-reload
        systemctl restart docker

        log "Docker 安装完成: $(docker --version)"
    fi
}

install_docker

# ============================================================
# 3. 安装 Docker Compose
# ============================================================
if docker compose version &>/dev/null; then
    log "Docker Compose 已安装: $(docker compose version)"
elif command -v docker-compose &>/dev/null; then
    log "Docker Compose (旧版) 已安装: $(docker-compose --version)"
else
    log "安装 Docker Compose 插件..."
    yum install -y docker-compose-plugin
    log "Docker Compose 安装完成"
fi

# ============================================================
# 4. 安装 Git
# ============================================================
if ! command -v git &>/dev/null; then
    log "安装 Git..."
    yum install -y git
fi

# ============================================================
# 5. 配置项目
# ============================================================
INSTALL_DIR="/opt/wms"

log "项目将安装到: ${INSTALL_DIR}"

# 如果目录已存在，询问是否覆盖
if [ -d "${INSTALL_DIR}" ]; then
    warn "目录 ${INSTALL_DIR} 已存在"
    read -p "是否删除并重新安装? (y/N): " confirm
    if [ "$confirm" = "y" ] || [ "$confirm" = "Y" ]; then
        rm -rf "${INSTALL_DIR}"
    else
        error "安装取消"
    fi
fi

# 克隆项目
log "克隆项目..."
git clone https://github.com/YiWeiLai233/wms.git "${INSTALL_DIR}"
cd "${INSTALL_DIR}"

# 切换到部署分支
git checkout feature/redis-cache

# ============================================================
# 6. 生成环境变量配置
# ============================================================
log "生成环境配置..."

# 生成随机密码
MYSQL_PWD=$(openssl rand -base64 16 | tr -dc 'a-zA-Z0-9' | head -c 16)
JWT_SECRET=$(openssl rand -base64 32 | tr -dc 'a-zA-Z0-9' | head -c 32)
AI_TOKEN=$(openssl rand -base64 16 | tr -dc 'a-zA-Z0-9' | head -c 16)

cat > .env <<EOF
# MySQL root 密码
MYSQL_ROOT_PASSWORD=Wms@${MYSQL_PWD}

# Redis 密码（默认无密码）
REDIS_PASSWORD=

# JWT 密钥
JWT_SECRET=Wms${JWT_SECRET}2026

# AI 服务 Token
AI_SERVICE_TOKEN=${AI_TOKEN}
LLM_PROVIDER=deepseek
LLM_API_KEY=
LLM_BASE_URL=https://api.deepseek.com
LLM_MODEL=deepseek-chat
EMBEDDING_MODEL=BAAI/bge-small-zh-v1.5
EOF

chmod 600 .env

# ============================================================
# 7. 开放防火墙端口
# ============================================================
if command -v firewall-cmd &>/dev/null; then
    log "配置防火墙..."
    firewall-cmd --permanent --add-port=80/tcp 2>/dev/null || true
    firewall-cmd --permanent --add-port=8080/tcp 2>/dev/null || true
    firewall-cmd --reload 2>/dev/null || true
    log "防火墙已开放 80, 8080 端口"
fi

# ============================================================
# 8. 启动服务
# ============================================================
log "启动 WMS 服务（首次构建约 5-10 分钟）..."
docker compose up -d --build

# 等待服务启动
log "等待服务启动..."
sleep 10

# 检查服务状态
log "检查服务状态..."
docker compose ps

# 等待 MySQL 就绪
log "等待 MySQL 就绪..."
for i in $(seq 1 60); do
    if docker compose exec -T mysql mysqladmin ping -h localhost -uroot -p"Wms@${MYSQL_PWD}" 2>/dev/null | grep -q "alive"; then
        log "MySQL 已就绪"
        break
    fi
    if [ "$i" -eq 60 ]; then
        warn "MySQL 启动超时，请检查日志: docker compose logs mysql"
    fi
    sleep 2
done

# ============================================================
# 9. 安装完成
# ============================================================
SERVER_IP=$(hostname -I | awk '{print $1}')

echo ""
echo "============================================================"
echo -e "${GREEN}  WMS 系统安装完成！${NC}"
echo "============================================================"
echo ""
echo "  前端访问:    http://${SERVER_IP}"
echo "  后端 API:    http://${SERVER_IP}:8080"
echo ""
echo "  默认账号:    admin"
echo "  默认密码:    123456"
echo ""
echo "  MySQL 密码:  Wms@${MYSQL_PWD}"
echo "  JWT 密钥:    Wms${JWT_SECRET}2026"
echo ""
echo "  安装目录:    ${INSTALL_DIR}"
echo "  环境配置:    ${INSTALL_DIR}/.env"
echo ""
echo "  常用命令:"
echo "    查看状态:  cd ${INSTALL_DIR} && docker compose ps"
echo "    查看日志:  cd ${INSTALL_DIR} && docker compose logs -f"
echo "    重启服务:  cd ${INSTALL_DIR} && docker compose restart"
echo "    停止服务:  cd ${INSTALL_DIR} && docker compose down"
echo "    更新部署:  cd ${INSTALL_DIR} && git pull && docker compose up -d --build"
echo ""
echo "============================================================"
