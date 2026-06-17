#!/bin/bash
# ============================================================
# WMS 系统 CentOS 一键部署脚本（无需 clone 代码）
# 用法: sudo bash install-standalone.sh
# 前提: 将 wms-deploy.tar.gz 放在同目录下
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
    error "请使用 root 用户执行: sudo bash install-standalone.sh"
fi

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
TARBALL="${SCRIPT_DIR}/wms-deploy.tar.gz"

if [ ! -f "${TARBALL}" ]; then
    error "未找到部署包 wms-deploy.tar.gz，请将其放在脚本同目录下"
fi

# ============================================================
# 1. 安装 Docker
# ============================================================
install_docker() {
    if command -v docker &>/dev/null; then
        log "Docker 已安装: $(docker --version)"
        return
    fi

    log "安装 Docker..."
    yum remove -y docker docker-client docker-client-latest docker-common \
        docker-latest docker-latest-logrotate docker-logrotate docker-engine 2>/dev/null || true
    yum install -y yum-utils device-mapper-persistent-data lvm2
    yum-config-manager --add-repo https://mirrors.aliyun.com/docker-ce/linux/centos/docker-ce.repo
    yum install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin

    mkdir -p /etc/docker
    cat > /etc/docker/daemon.json <<'EOF'
{
    "registry-mirrors": [
        "https://docker.1ms.run",
        "https://docker.xuanyuan.me"
    ],
    "log-driver": "json-file",
    "log-opts": { "max-size": "10m", "max-file": "3" }
}
EOF
    systemctl start docker
    systemctl enable docker
    systemctl daemon-reload
    systemctl restart docker
    log "Docker 安装完成"
}

install_docker

# ============================================================
# 2. 解压部署包
# ============================================================
INSTALL_DIR="/opt/wms"

if [ -d "${INSTALL_DIR}" ]; then
    warn "目录 ${INSTALL_DIR} 已存在"
    read -p "是否删除并重新部署? (y/N): " confirm
    if [ "$confirm" != "y" ] && [ "$confirm" != "Y" ]; then
        error "部署取消"
    fi
    cd "${INSTALL_DIR}" && docker compose down 2>/dev/null || true
    rm -rf "${INSTALL_DIR}"
fi

log "解压部署包..."
mkdir -p "${INSTALL_DIR}"
tar -xzf "${TARBALL}" -C "${INSTALL_DIR}"
cd "${INSTALL_DIR}"

# ============================================================
# 3. 生成环境变量
# ============================================================
log "生成环境配置..."

MYSQL_PWD=$(openssl rand -base64 16 | tr -dc 'a-zA-Z0-9' | head -c 16)
JWT_SECRET=$(openssl rand -base64 32 | tr -dc 'a-zA-Z0-9' | head -c 32)

cat > .env <<EOF
MYSQL_ROOT_PASSWORD=Wms@${MYSQL_PWD}
REDIS_PASSWORD=
JWT_SECRET=Wms${JWT_SECRET}2026
EOF
chmod 600 .env

# ============================================================
# 4. 防火墙
# ============================================================
if command -v firewall-cmd &>/dev/null; then
    log "配置防火墙..."
    firewall-cmd --permanent --add-port=80/tcp 2>/dev/null || true
    firewall-cmd --permanent --add-port=8080/tcp 2>/dev/null || true
    firewall-cmd --reload 2>/dev/null || true
fi

# ============================================================
# 5. 启动
# ============================================================
log "启动服务（首次构建约 3-5 分钟）..."
docker compose up -d --build

log "等待 MySQL 就绪..."
for i in $(seq 1 60); do
    if docker compose exec -T mysql mysqladmin ping -h localhost -uroot -p"Wms@${MYSQL_PWD}" 2>/dev/null | grep -q "alive"; then
        log "MySQL 已就绪"
        break
    fi
    [ "$i" -eq 60 ] && warn "MySQL 启动超时，请检查: docker compose logs mysql"
    sleep 2
done

# ============================================================
# 6. 完成
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
    查看状态:  cd ${INSTALL_DIR} && docker compose ps
    查看日志:  cd ${INSTALL_DIR} && docker compose logs -f
    重启服务:  cd ${INSTALL_DIR} && docker compose restart
    停止服务:  cd ${INSTALL_DIR} && docker compose down

============================================================
INFO
