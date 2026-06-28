#!/usr/bin/env bash
# crm-service 원클릭 Docker 배포 (Git Bash / WSL / Linux)
#
# 사용:
#   ./deploy.sh
#   ./deploy.sh --skip-build
#   ./deploy.sh --port 8084
#
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
SERVICE_DIR="${SCRIPT_DIR}"
SERVICE_NAME="crm-service"
IMAGE_NAME="crm-service:latest"
TAR_NAME="crm-service-image.tar"

SERVER_HOST="${DEPLOY_SERVER_HOST:-192.168.55.223}"
SERVER_USER="${DEPLOY_SERVER_USER:-cwuser}"
REMOTE_DEPLOY_DIR="/home/cwuser/msa/deploy"
DOCKER_NETWORK="${DOCKER_NETWORK:-msa-net}"
SPRING_PROFILE="${SPRING_PROFILES_ACTIVE:-prod}"
HOST_PORT="${CRM_PORT:-8084}"
CONTAINER_PORT="${CRM_PORT:-8084}"
SKIP_BUILD=0

while [[ $# -gt 0 ]]; do
  case "$1" in
    --skip-build) SKIP_BUILD=1; shift ;;
    --port)
      HOST_PORT="$2"
      CONTAINER_PORT="$2"
      shift 2
      ;;
    *) echo "[ERROR] Unknown option: $1" >&2; exit 1 ;;
  esac
done

log()  { echo "[$(date '+%H:%M:%S')] $*"; }
fail() { echo "[ERROR] $*" >&2; exit 1; }

command -v ssh >/dev/null 2>&1 || fail "ssh not found"
command -v scp >/dev/null 2>&1 || fail "scp not found"
command -v docker >/dev/null 2>&1 || fail "docker not found"

log "========================================"
log " CRM Service Docker Deploy"
log " Server : ${SERVER_USER}@${SERVER_HOST}"
log " Port   : ${HOST_PORT}"
log " Profile: ${SPRING_PROFILE}"
log "========================================"

if [[ "$SKIP_BUILD" -eq 0 ]]; then
  log "[1/4] Gradle bootJar build..."
  (cd "$SERVICE_DIR" && ./gradlew clean bootJar)
  log "[OK] build complete"
else
  log "[1/4] skip build (--skip-build)"
fi

log "[2/4] Docker image build..."
docker build -t "$IMAGE_NAME" "$SERVICE_DIR"
log "[OK] image: $IMAGE_NAME"

log "[3/4] Upload deploy script and image..."
ssh "${SERVER_USER}@${SERVER_HOST}" "mkdir -p '${REMOTE_DEPLOY_DIR}'"
scp "${PROJECT_ROOT}/deploy/deploy-crm-remote.sh" "${SERVER_USER}@${SERVER_HOST}:${REMOTE_DEPLOY_DIR}/"
ssh "${SERVER_USER}@${SERVER_HOST}" "chmod +x '${REMOTE_DEPLOY_DIR}/deploy-crm-remote.sh'"

docker save -o "${SERVICE_DIR}/${TAR_NAME}" "$IMAGE_NAME"
scp "${SERVICE_DIR}/${TAR_NAME}" "${SERVER_USER}@${SERVER_HOST}:${REMOTE_DEPLOY_DIR}/"
rm -f "${SERVICE_DIR}/${TAR_NAME}"
log "[OK] image uploaded"

log "[4/4] Recreate remote container..."
ssh "${SERVER_USER}@${SERVER_HOST}" \
  "bash ${REMOTE_DEPLOY_DIR}/deploy-crm-remote.sh ${HOST_PORT} ${CONTAINER_PORT} ${SPRING_PROFILE} ${DOCKER_NETWORK}"

log "========================================"
log " Deploy complete"
log " Gateway CRM : http://${SERVER_HOST}:8081/crm-service/"
log " Direct API  : http://${SERVER_HOST}:${HOST_PORT}/leads"
log " Deal Stats  : http://${SERVER_HOST}:${HOST_PORT}/deals/dashboard/stats"
log "========================================"
