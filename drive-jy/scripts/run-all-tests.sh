#!/usr/bin/env bash
# =============================================================================
# run-all-tests.sh — JUnit + E2E 전체 테스트 자동 실행 스크립트
#
# 사용법:
#   bash scripts/run-all-tests.sh            # 전체 (JUnit + E2E)
#   bash scripts/run-all-tests.sh --junit    # JUnit만
#   bash scripts/run-all-tests.sh --e2e      # E2E만 (서버 자동 기동 포함)
#   bash scripts/run-all-tests.sh --skip-junit  # JUnit 건너뛰고 E2E만
# =============================================================================

set -euo pipefail

# ── 색상 출력 헬퍼 ────────────────────────────────────────────────────────────
RED='\033[0;31m'; GREEN='\033[0;32m'; YELLOW='\033[1;33m'
BLUE='\033[0;34m'; BOLD='\033[1m'; NC='\033[0m'

info()    { echo -e "${BLUE}[INFO]${NC}  $*"; }
ok()      { echo -e "${GREEN}[OK]${NC}    $*"; }
warn()    { echo -e "${YELLOW}[WARN]${NC}  $*"; }
error()   { echo -e "${RED}[ERROR]${NC} $*"; }
section() { echo -e "\n${BOLD}══════════════════════════════════════════${NC}"; echo -e "${BOLD} $*${NC}"; echo -e "${BOLD}══════════════════════════════════════════${NC}"; }

# ── 프로젝트 루트로 이동 ──────────────────────────────────────────────────────
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
cd "$PROJECT_ROOT"

# ── 옵션 파싱 ─────────────────────────────────────────────────────────────────
RUN_JUNIT=true
RUN_E2E=true

for arg in "$@"; do
  case $arg in
    --junit)      RUN_JUNIT=true;  RUN_E2E=false ;;
    --e2e)        RUN_JUNIT=false; RUN_E2E=true  ;;
    --skip-junit) RUN_JUNIT=false; RUN_E2E=true  ;;
    --help|-h)
      echo "사용법: bash scripts/run-all-tests.sh [옵션]"
      echo "  (옵션 없음)    JUnit + E2E 전체 실행"
      echo "  --junit        JUnit 테스트만 실행"
      echo "  --e2e          E2E 테스트만 실행 (서버 자동 기동)"
      echo "  --skip-junit   JUnit 건너뛰고 E2E만 실행"
      exit 0
      ;;
    *) warn "알 수 없는 옵션: $arg (무시)"; ;;
  esac
done

# ── 결과 추적 ─────────────────────────────────────────────────────────────────
JUNIT_RESULT="SKIP"
E2E_RESULT="SKIP"
SERVER_PID=""
SERVER_STARTED=false

# ── 서버 포트 확인 ────────────────────────────────────────────────────────────
SERVER_PORT=8080

server_running() {
  curl -s "http://localhost:${SERVER_PORT}/auth/login" 2>/dev/null | grep -q "로그인" 2>/dev/null
}

server_pid_on_port() {
  netstat -ano 2>/dev/null | grep ":${SERVER_PORT}.*LISTENING" | awk '{print $5}' | head -1
}

stop_server() {
  if [[ "$SERVER_STARTED" == "true" && -n "$SERVER_PID" ]]; then
    info "서버 종료 중 (PID: $SERVER_PID)..."
    # Windows: taskkill, Linux/Mac: kill
    if command -v taskkill &>/dev/null; then
      taskkill //F //PID "$SERVER_PID" &>/dev/null || true
    else
      kill "$SERVER_PID" 2>/dev/null || true
    fi
    # 포트가 남아있으면 강제 종료
    local port_pid
    port_pid=$(server_pid_on_port)
    if [[ -n "$port_pid" ]]; then
      if command -v taskkill &>/dev/null; then
        taskkill //F //PID "$port_pid" &>/dev/null || true
      else
        kill -9 "$port_pid" 2>/dev/null || true
      fi
    fi
    ok "서버 종료 완료"
  fi
}

# 스크립트 종료 시 서버 정리
trap 'stop_server' EXIT

# ── Gradle 래퍼 결정 ──────────────────────────────────────────────────────────
if [[ -f "./gradlew.bat" && "$OSTYPE" == "msys"* ]]; then
  GRADLEW="./gradlew.bat"
elif [[ -f "./gradlew" ]]; then
  GRADLEW="./gradlew"
else
  error "Gradle 래퍼를 찾을 수 없습니다."
  exit 1
fi

# ══════════════════════════════════════════════════════════════════════════════
# 1단계: JUnit 테스트
# ══════════════════════════════════════════════════════════════════════════════
if [[ "$RUN_JUNIT" == "true" ]]; then
  section "1단계: JUnit 테스트"
  info "Gradle 테스트 실행 중..."

  if $GRADLEW test 2>&1 | tee /tmp/junit-output.txt; then
    ok "JUnit 테스트 통과"
    JUNIT_RESULT="PASS"
  else
    error "JUnit 테스트 실패"
    JUNIT_RESULT="FAIL"
    # JUnit 실패 시 E2E 건너뜀
    warn "JUnit 실패로 E2E 테스트를 건너뜁니다."
    RUN_E2E=false
  fi
fi

# ══════════════════════════════════════════════════════════════════════════════
# 2단계: 서버 기동 (E2E 필요 시)
# ══════════════════════════════════════════════════════════════════════════════
if [[ "$RUN_E2E" == "true" ]]; then
  section "2단계: 서버 기동 확인"

  if server_running; then
    ok "서버가 이미 실행 중입니다 (포트 $SERVER_PORT)"
    SERVER_STARTED=false
  else
    info "서버를 시작합니다..."

    # 포트에 남은 프로세스 정리
    existing_pid=$(server_pid_on_port)
    if [[ -n "$existing_pid" ]]; then
      warn "포트 $SERVER_PORT 선점 프로세스(PID $existing_pid) 종료 중..."
      if command -v taskkill &>/dev/null; then
        taskkill //F //PID "$existing_pid" &>/dev/null || true
      else
        kill -9 "$existing_pid" 2>/dev/null || true
      fi
      sleep 2
    fi

    # 서버 백그라운드 시작
    $GRADLEW bootRun > /tmp/bootrun-test.log 2>&1 &
    SERVER_PID=$!
    SERVER_STARTED=true
    info "서버 시작 중... (PID: $SERVER_PID)"

    # 최대 90초 대기
    WAIT_SECONDS=90
    for i in $(seq 1 $WAIT_SECONDS); do
      if server_running; then
        ok "서버 준비 완료 (${i}초 소요)"
        break
      fi
      if [[ $i -eq $WAIT_SECONDS ]]; then
        error "서버가 ${WAIT_SECONDS}초 내에 시작되지 않았습니다."
        error "로그 확인: /tmp/bootrun-test.log"
        tail -20 /tmp/bootrun-test.log || true
        exit 1
      fi
      sleep 1
    done
  fi
fi

# ══════════════════════════════════════════════════════════════════════════════
# 3단계: E2E 테스트
# ══════════════════════════════════════════════════════════════════════════════
if [[ "$RUN_E2E" == "true" ]]; then
  section "3단계: E2E 테스트 (Playwright)"

  E2E_DIR="$PROJECT_ROOT/e2e"
  if [[ ! -d "$E2E_DIR" ]]; then
    error "e2e 디렉토리를 찾을 수 없습니다: $E2E_DIR"
    E2E_RESULT="FAIL"
  else
    cd "$E2E_DIR"
    info "Playwright 테스트 실행 중..."

    if npx playwright test --reporter=line 2>&1 | tee /tmp/e2e-output.txt; then
      ok "E2E 테스트 통과"
      E2E_RESULT="PASS"
    else
      error "E2E 테스트 실패"
      E2E_RESULT="FAIL"
    fi

    cd "$PROJECT_ROOT"
  fi
fi

# ══════════════════════════════════════════════════════════════════════════════
# 최종 결과 보고
# ══════════════════════════════════════════════════════════════════════════════
section "테스트 결과 요약"

print_result() {
  local label="$1"
  local result="$2"
  case $result in
    PASS) echo -e "  ${GREEN}✔${NC} ${label}: ${GREEN}PASS${NC}" ;;
    FAIL) echo -e "  ${RED}✘${NC} ${label}: ${RED}FAIL${NC}" ;;
    SKIP) echo -e "  ${YELLOW}–${NC} ${label}: ${YELLOW}SKIP${NC}" ;;
  esac
}

print_result "JUnit 테스트" "$JUNIT_RESULT"
print_result "E2E 테스트  " "$E2E_RESULT"
echo ""

# 전체 성공/실패 판정
if [[ "$JUNIT_RESULT" == "FAIL" || "$E2E_RESULT" == "FAIL" ]]; then
  error "일부 테스트가 실패했습니다."
  exit 1
else
  ok "모든 테스트가 통과했습니다."
  exit 0
fi
