#!/usr/bin/env bash
# =============================================================================
# MSA 도커 서버 재시작 스크립트
# 서버: 192.168.55.223 (cwuser)
#
# 사전 조건:
#   - PC: deploy/sync-config.ps1  → /data/config-repo 동기화
#   - PC: deploy/copy-jars.ps1    → /home/cwuser/msa/jars 복사
#
# 사용 (서버에서):
#   chmod +x deploy/server-restart-services.sh
#   ./deploy/server-restart-services.sh              # 기존 컨테이너 restart
#   DEPLOY_MODE=recreate ./deploy/server-restart-services.sh  # JAR 마운트 재생성
#
# 프로파일: SPRING_PROFILES_ACTIVE=prod
# 네트워크: msa-net (user-db 포함)
# 포트    : eureka 8761 | config 8888 | gateway 8081 | user 8082 | login 8088 | crm 8084
# =============================================================================

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# deploy.env 는 PC(C:\MSA\deploy) 또는 서버(/home/cwuser/msa/deploy) 양쪽에 둘 수 있음
if [[ -f "${SCRIPT_DIR}/deploy.env" ]]; then
  # shellcheck source=deploy.env
  source "${SCRIPT_DIR}/deploy.env"
else
  DEPLOY_SERVER_HOST="192.168.55.223"
  CONFIG_REPO_HOST="/data/config-repo"
  JAR_DIR="/home/cwuser/msa/jars"
  DOCKER_NETWORK="msa-net"
  SPRING_PROFILES_ACTIVE="prod"
  JAVA_IMAGE="eclipse-temurin:17-jre"
  JAVA_OPTS="-Xmx512m"
  declare -A SERVICE_PORTS=(
    ["eureka-server"]=8761
    ["config-server"]=8888
    ["user-service"]=8082
    ["login-service"]=8088
    ["gateway-service"]=8081
    ["crm-service"]=8084
  )
  STARTUP_ORDER=("eureka-server" "config-server" "user-service" "login-service" "gateway-service" "crm-service")
  NETWORK_CONTAINERS=("user-db" "eureka-server" "config-server" "user-service" "login-service" "gateway-service" "crm-service")
fi

DEPLOY_MODE="${DEPLOY_MODE:-restart}"   # restart | recreate
WAIT_SECONDS="${WAIT_SECONDS:-5}"

log()  { echo "[$(date '+%H:%M:%S')] $*"; }
fail() { echo "[ERROR] $*" >&2; exit 1; }

container_exists() {
  docker ps -a --format '{{.Names}}' | grep -qx "$1"
}

container_running() {
  docker ps --format '{{.Names}}' | grep -qx "$1"
}

ensure_network() {
  log "Docker network 확인: ${DOCKER_NETWORK}"
  if ! docker network inspect "${DOCKER_NETWORK}" >/dev/null 2>&1; then
    docker network create "${DOCKER_NETWORK}"
    log "네트워크 생성: ${DOCKER_NETWORK}"
  fi

  for c in "${NETWORK_CONTAINERS[@]}"; do
    if container_exists "$c"; then
      docker network connect "${DOCKER_NETWORK}" "$c" 2>/dev/null || true
      log "네트워크 연결: $c"
    fi
  done
}

wait_for_port() {
  local host="$1"
  local port="$2"
  local retries="${3:-30}"
  local i=0

  while (( i < retries )); do
    if (echo >"/dev/tcp/${host}/${port}") >/dev/null 2>&1; then
      return 0
    fi
    sleep 1
    ((i++))
  done
  return 1
}

restart_existing() {
  local name="$1"

  if ! container_exists "$name"; then
    log "[SKIP] 컨테이너 없음: $name"
    return 0
  fi

  # prod 프로파일 주입 (이미 있으면 무시될 수 있음)
  docker update --env-add "SPRING_PROFILES_ACTIVE=${SPRING_PROFILES_ACTIVE}" "$name" 2>/dev/null || true
  docker restart "$name" >/dev/null
  log "[OK] restarted: $name"
}

recreate_java_service() {
  local name="$1"
  local port="$2"
  local jar="${JAR_DIR}/${name}.jar"
  local needs_config="${3:-false}"

  if [[ ! -f "$jar" ]]; then
    log "[SKIP] JAR 없음: $jar"
    return 0
  fi

  docker rm -f "$name" 2>/dev/null || true

  local -a run_args=(
    -d
    --name "$name"
    --hostname "$name"
    --network "${DOCKER_NETWORK}"
    --restart unless-stopped
    -p "${port}:${port}"
    -e "SPRING_PROFILES_ACTIVE=${SPRING_PROFILES_ACTIVE}"
    -e "JAVA_TOOL_OPTIONS=${JAVA_OPTS}"
    -v "${jar}:/app/app.jar:ro"
  )

  if [[ "$needs_config" == "true" ]]; then
    run_args+=(-v "${CONFIG_REPO_HOST}:${CONFIG_REPO_HOST}:ro")
  fi

  docker run "${run_args[@]}" "${JAVA_IMAGE}" java -jar /app/app.jar >/dev/null
  log "[OK] recreated: $name (port ${port}, profile ${SPRING_PROFILES_ACTIVE})"
}

deploy_service() {
  local name="$1"
  local port="${SERVICE_PORTS[$name]}"

  if [[ "$DEPLOY_MODE" == "recreate" ]]; then
  case "$name" in
    eureka-server)
      recreate_java_service "$name" "$port" false
      ;;
    config-server)
      recreate_java_service "$name" "$port" true
      ;;
    user-service|login-service|gateway-service|crm-service)
      recreate_java_service "$name" "$port" false
      ;;
    *)
      log "[SKIP] unknown service: $name"
      ;;
  esac
  else
    restart_existing "$name"
  fi
}

main() {
  command -v docker >/dev/null 2>&1 || fail "docker 명령을 찾을 수 없습니다."

  log "========================================"
  log " MSA Server Restart"
  log " Host    : ${DEPLOY_SERVER_HOST:-local}"
  log " Profile : ${SPRING_PROFILES_ACTIVE}"
  log " Network : ${DOCKER_NETWORK}"
  log " Mode    : ${DEPLOY_MODE}"
  log " JAR dir : ${JAR_DIR}"
  log " Config  : ${CONFIG_REPO_HOST}"
  log "========================================"

  ensure_network

  for svc in "${STARTUP_ORDER[@]}"; do
    log "--- deploy: $svc ---"
    deploy_service "$svc"

    case "$svc" in
      eureka-server)
        sleep "${WAIT_SECONDS}"
        wait_for_port "127.0.0.1" "${SERVICE_PORTS[eureka-server]}" 60 \
          && log "Eureka 준비 완료" \
          || log "[WARN] Eureka 포트 대기 타임아웃"
        ;;
      config-server)
        sleep "${WAIT_SECONDS}"
        wait_for_port "127.0.0.1" "${SERVICE_PORTS[config-server]}" 60 \
          && log "Config Server 준비 완료" \
          || log "[WARN] Config Server 포트 대기 타임아웃"
        ;;
    esac
  done

  log "========================================"
  log " 배포 완료"
  log " Eureka  : http://${DEPLOY_SERVER_HOST}:8761"
  log " Gateway : http://${DEPLOY_SERVER_HOST}:8081"
  log " User    : http://${DEPLOY_SERVER_HOST}:8082/test"
  log " Login   : http://${DEPLOY_SERVER_HOST}:8088"
  log " CRM     : http://${DEPLOY_SERVER_HOST}:8081/crm-service/  (내부 ${SERVICE_PORTS[crm-service]})"
  log "========================================"
}

main "$@"
