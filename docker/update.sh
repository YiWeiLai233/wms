#!/bin/bash
# ============================================================
# WMS 更新脚本 - 只更新前后端，不动 MySQL/Redis/数据/配置
# 用法: sudo bash /root/wms/update.sh /path/to/wms-deploy.tar.gz
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
    error "请使用 root 用户执行: sudo bash update.sh /path/to/wms-deploy.tar.gz"
fi

INSTALL_DIR="/root/wms"
TARBALL="$1"

if [ -z "${TARBALL}" ]; then
    error "请指定部署包路径: sudo bash update.sh /path/to/wms-deploy.tar.gz"
fi

if [ ! -f "${TARBALL}" ]; then
    error "部署包不存在: ${TARBALL}"
fi

if [ ! -f "${INSTALL_DIR}/.env" ]; then
    error "未找到 ${INSTALL_DIR}/.env，请先用 install-standalone.sh 首次部署"
fi

# ============================================================
# 1. 解压到临时目录
# ============================================================
TMPDIR=$(mktemp -d)
trap "rm -rf ${TMPDIR}" EXIT

log "解压部署包..."
tar -xzf "${TARBALL}" -C "${TMPDIR}"

# ============================================================
# 2. 加载新镜像
# ============================================================
if [ -f "${TMPDIR}/wms-backend.tar" ]; then
    log "加载后端镜像..."
    docker load -i "${TMPDIR}/wms-backend.tar"
else
    warn "未找到 wms-backend.tar，跳过后端更新"
fi

if [ -f "${TMPDIR}/wms-frontend.tar" ]; then
    log "加载前端镜像..."
    docker load -i "${TMPDIR}/wms-frontend.tar"
else
    warn "未找到 wms-frontend.tar，跳过前端更新"
fi

# ============================================================
# 3. 更新 docker-compose.yml（可选覆盖）
# ============================================================
if [ -f "${TMPDIR}/docker-compose.yml" ]; then
    log "更新 docker-compose.yml..."
    cp "${TMPDIR}/docker-compose.yml" "${INSTALL_DIR}/docker-compose.yml"
fi

# ============================================================
# 4. 重启前后端容器
# ============================================================
cd "${INSTALL_DIR}"

log "重启后端..."
docker compose up -d --force-recreate wms-backend

log "等待后端启动..."
for i in $(seq 1 30); do
    if curl -sf http://localhost:8080/actuator/health >/dev/null 2>&1; then
        log "后端已就绪"
        break
    fi
    [ "$i" -eq 30 ] && warn "后端启动较慢，请检查: docker compose logs wms-backend"
    sleep 2
done

log "重启前端..."
docker compose up -d --force-recreate wms-frontend

# ============================================================
# 5. 清理悬空镜像
# ============================================================
log "清理旧镜像..."
docker image prune -f >/dev/null 2>&1 || true

# ============================================================
# 6. 完成
# ============================================================
SERVER_IP=$(hostname -I | awk '{print $1}')

cat <<INFO

============================================================
  WMS 更新完成！
============================================================

  前端访问:    http://${SERVER_IP}
  后端 API:    http://${SERVER_IP}:8080

  常用命令:
    查看状态:  cd ${INSTALL_DIR} && docker compose ps
    查看日志:  cd ${INSTALL_DIR} && docker compose logs -f

============================================================
INFO
