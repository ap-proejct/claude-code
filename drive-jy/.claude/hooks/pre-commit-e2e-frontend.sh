#!/usr/bin/env bash
# pre-commit-e2e-frontend.sh — 재설치용 소스 파일
#
# .git/hooks/pre-commit 에 설치해야 실제 동작한다.
# 설치 명령:
#   cp .claude/hooks/pre-commit-e2e-frontend.sh .git/hooks/pre-commit
#   chmod +x .git/hooks/pre-commit
#
# 기준 파일: e2e/test-results/.last-run.json (npx playwright test 실행마다 갱신)
# 조건: 마지막 커밋 이후 E2E가 실행됐고 status == "passed"

set -euo pipefail

STAGED_HTML=$(git diff --cached --name-only 2>/dev/null | grep 'templates/.*\.html$' || true)

if [ -z "$STAGED_HTML" ]; then
  exit 0
fi

LAST_RUN="e2e/test-results/.last-run.json"

if [ ! -f "$LAST_RUN" ]; then
  echo "" >&2
  echo "❌ E2E 테스트 미실행 — 커밋 차단" >&2
  echo "" >&2
  echo "  HTML 템플릿이 변경되었지만 E2E 테스트 결과가 없습니다." >&2
  echo "" >&2
  echo "  변경된 파일:" >&2
  echo "$STAGED_HTML" | while IFS= read -r f; do echo "    - $f" >&2; done
  echo "" >&2
  echo "  실행 순서:" >&2
  echo "    1. ./gradlew.bat bootRun    ← 별도 터미널에서 서버 기동" >&2
  echo "    2. cd e2e && npx playwright test" >&2
  echo "    3. git commit 재시도" >&2
  echo "" >&2
  exit 1
fi

# E2E 결과가 마지막 커밋보다 최신인지 확인
LAST_COMMIT_TIME=$(git log -1 --format=%ct 2>/dev/null || echo 0)
LAST_RUN_TIME=$(stat -c %Y "$LAST_RUN" 2>/dev/null || python3 -c "import os; print(int(os.path.getmtime('$LAST_RUN')))" 2>/dev/null || echo 0)

if [ "$LAST_RUN_TIME" -le "$LAST_COMMIT_TIME" ]; then
  echo "" >&2
  echo "❌ 마지막 커밋 이후 E2E 미실행 — 커밋 차단" >&2
  echo "" >&2
  echo "  HTML 템플릿이 변경되었지만 마지막 커밋 이후 E2E가 실행되지 않았습니다." >&2
  echo "" >&2
  echo "  실행 순서:" >&2
  echo "    1. ./gradlew.bat bootRun    ← 별도 터미널에서 서버 기동" >&2
  echo "    2. cd e2e && npx playwright test" >&2
  echo "    3. git commit 재시도" >&2
  echo "" >&2
  exit 1
fi

# E2E 결과가 passed 상태인지 확인
E2E_STATUS=$(python3 -c "import json; d=json.load(open('$LAST_RUN')); print(d.get('status',''))" 2>/dev/null || echo "unknown")

if [ "$E2E_STATUS" != "passed" ]; then
  echo "" >&2
  echo "❌ E2E 테스트 실패 — 커밋 차단" >&2
  echo "" >&2
  echo "  마지막 E2E 실행 결과: $E2E_STATUS" >&2
  echo "  E2E 테스트를 통과시킨 후 커밋하세요." >&2
  echo "" >&2
  exit 1
fi

exit 0
