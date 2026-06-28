#!/usr/bin/env bash
# 원격 서버(192.168.55.223)에서 crm-service Docker 컨테이너를 재생성합니다.
# deploy.bat / deploy.sh 에서 ssh 로 호출됩니다.
#
# 사용 (서버에서 직접):
#   bash /home/cwuser/msa/deploy/deploy-crm-remote.sh [host_port] [container_port] [profile] [network]
#
set -euo pipefail

HOST_PORT="${1:-8084}"
CONTAINER_PORT="${2:-8084}"
SPRING_PROFILE="${3:-prod}"
DOCKER_NETWORK="${4:-msa-net}"

CONTAINER_NAME="crm-service"
IMAGE_NAME="crm-service:latest"
TAR_PATH="/home/cwuser/msa/deploy/crm-service-image.tar"

# prod: Docker 내부 user-db 컨테이너 사용
DB_URL="${SPRING_DATASOURCE_URL:-jdbc:postgresql://user-db:5432/user_db}"
DB_USER="${SPRING_DATASOURCE_USERNAME:?SPRING_DATASOURCE_USERNAME is required}"
DB_PASS="${SPRING_DATASOURCE_PASSWORD:?SPRING_DATASOURCE_PASSWORD is required}"

log()  { echo "[$(date '+%H:%M:%S')] $*"; }
fail() { echo "[ERROR] $*" >&2; exit 1; }

command -v docker >/dev/null 2>&1 || fail "docker not found"

if [[ ! -f "$TAR_PATH" ]]; then
  fail "Image tar not found: $TAR_PATH (run deploy.bat from local first)"
fi

log "Load Docker image..."
docker load -i "$TAR_PATH"

if ! docker network inspect "$DOCKER_NETWORK" >/dev/null 2>&1; then
  log "Create network: $DOCKER_NETWORK"
  docker network create "$DOCKER_NETWORK"
fi

log "Stop/remove existing container: $CONTAINER_NAME"
docker rm -f "$CONTAINER_NAME" 2>/dev/null || true

log "Start new container (port ${HOST_PORT}:${CONTAINER_PORT})..."
docker run -d \
  --name "$CONTAINER_NAME" \
  --hostname "$CONTAINER_NAME" \
  --network "$DOCKER_NETWORK" \
  --restart unless-stopped \
  -p "${HOST_PORT}:${CONTAINER_PORT}" \
  -e "SPRING_PROFILES_ACTIVE=${SPRING_PROFILE}" \
  -e "SERVER_PORT=${CONTAINER_PORT}" \
  -e "SPRING_DATASOURCE_URL=${DB_URL}" \
  -e "SPRING_DATASOURCE_USERNAME=${DB_USER}" \
  -e "SPRING_DATASOURCE_PASSWORD=${DB_PASS}" \
  -e "JAVA_TOOL_OPTIONS=-Xmx512m" \
  "$IMAGE_NAME"

# user-db 가 msa-net 에 없으면 연결 시도
if docker ps -a --format '{{.Names}}' | grep -qx "user-db"; then
  docker network connect "$DOCKER_NETWORK" user-db 2>/dev/null || true
fi

log "Deploy complete: $CONTAINER_NAME"
docker ps --filter "name=${CONTAINER_NAME}" --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"
