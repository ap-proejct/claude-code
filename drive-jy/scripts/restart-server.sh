#!/usr/bin/env bash
# =============================================================================
# restart-server.sh — 서버 중지 → processResources → bootRun 재시작
#
# 사용법:
#   bash scripts/restart-server.sh
#   bash scripts/restart-server.sh --wait   # 준비 완료까지 대기 후 반환
# =============================================================================

set -euo pipefail

RED='\033[0;31m'; GREEN='\033[0;32m'; YELLOW='\033[1;33m'
BLUE='\033[0;34m'; BOLD='\033[1m'; NC='\033[0m'

info()  { echo -e "${BLUE}[INFO]${NC}  $*"; }
ok()    { echo -e "${GREEN}[OK]${NC}    $*"; }
warn()  { echo -e "${YELLOW}[WARN]${NC}  $*"; }
error() { echo -e "${RED}[ERROR]${NC} $*"; }

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
cd "$PROJECT_ROOT"

SERVER_PORT=8080
WAIT_MODE=false
for arg in "$@"; do
  [[ "$arg" == "--wait" ]] && WAIT_MODE=true
done

# ── Gradle 래퍼 결정 ──────────────────────────────────────────────────────────
if [[ -f "./gradlew.bat" && "$OSTYPE" == "msys"* ]]; then
  GRADLEW="./gradlew.bat"
elif [[ -f "./gradlew" ]]; then
  GRADLEW="./gradlew"
else
  error "Gradle 래퍼를 찾을 수 없습니다."; exit 1
fi

# ── 기존 프로세스 종료 ────────────────────────────────────────────────────────
kill_port() {
  local pid
  pid=$(netstat -ano 2>/dev/null | grep ":${SERVER_PORT}.*LISTENING" | awk '{print $5}' | head -1)
  if [[ -n "$pid" ]]; then
    info "포트 ${SERVER_PORT} 선점 프로세스(PID ${pid}) 종료 중..."
    if command -v taskkill &>/dev/null; then
      taskkill //F //PID "$pid" &>/dev/null || true
    else
      kill -9 "$pid" 2>/dev/null || true
    fi
    sleep 2
    ok "기존 서버 종료 완료"
  else
    info "실행 중인 서버 없음"
  fi
}

kill_port

# ── 리소스 동기화 ─────────────────────────────────────────────────────────────
info "리소스 동기화 중 (processResources)..."
$GRADLEW processResources -q
ok "리소스 동기화 완료"

# ── 서버 백그라운드 시작 ──────────────────────────────────────────────────────
LOG_FILE="/tmp/bootrun-server.log"
info "서버 시작 중... (로그: ${LOG_FILE})"
$GRADLEW bootRun > "$LOG_FILE" 2>&1 &
SERVER_PID=$!
info "서버 PID: ${SERVER_PID}"

# ── 준비 대기 ─────────────────────────────────────────────────────────────────
if [[ "$WAIT_MODE" == "true" ]]; then
  WAIT_SECONDS=120
  info "서버 준비 대기 중 (최대 ${WAIT_SECONDS}초)..."
  for i in $(seq 1 $WAIT_SECONDS); do
    if curl -s "http://localhost:${SERVER_PORT}/auth/login" 2>/dev/null | grep -q "로그인"; then
      ok "서버 준비 완료 (${i}초 소요)"
      exit 0
    fi
    if [[ $i -eq $WAIT_SECONDS ]]; then
      error "서버가 ${WAIT_SECONDS}초 내에 시작되지 않았습니다."
      tail -20 "$LOG_FILE" || true
      exit 1
    fi
    sleep 1
  done
else
  ok "서버 백그라운드 시작됨 (PID: ${SERVER_PID})"
  info "준비 확인: curl -s http://localhost:${SERVER_PORT}/auth/login | grep 로그인"
fi
