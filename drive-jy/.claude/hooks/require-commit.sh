#!/bin/bash
# Stop hook
# 미커밋 .kt / templates/*.html 변경이 있으면 응답 종료 차단 (exit 2)

# git 미초기화 → 경고만 하고 통과
if [ ! -d ".git" ]; then
  echo "⚠️  git 저장소 없음. 'git init' 후 첫 커밋을 만들어야 커밋 강제가 활성화됩니다." >&2
  exit 0
fi

# 미커밋 .kt 파일 목록 추출
UNCOMMITTED_KT=$(git status --porcelain 2>/dev/null | grep '\.kt$' | awk '{print $2}')

# 미커밋 HTML 템플릿 파일 목록 추출
UNCOMMITTED_HTML=$(git status --porcelain 2>/dev/null | grep 'templates/.*\.html$' | awk '{print $2}')

if [ -n "$UNCOMMITTED_KT" ]; then
  echo "❌ 미커밋 .kt 파일이 있습니다 — 응답 종료 차단" >&2
  echo "" >&2
  echo "  다음 파일을 커밋해야 작업이 완료됩니다:" >&2
  echo "$UNCOMMITTED_KT" | while read -r f; do
    echo "    - $f" >&2
  done
  echo "" >&2
  echo "  완료 순서:" >&2
  echo "    1. ./gradlew.bat test              # 전체 JUnit 테스트" >&2
  echo "    2. git add <파일들>" >&2
  echo "    3. git commit -m \"<작업 내용>\"" >&2
  exit 2
fi

if [ -n "$UNCOMMITTED_HTML" ]; then
  echo "❌ 미커밋 HTML 템플릿 파일이 있습니다 — 응답 종료 차단" >&2
  echo "" >&2
  echo "  다음 파일을 커밋해야 작업이 완료됩니다:" >&2
  echo "$UNCOMMITTED_HTML" | while read -r f; do
    echo "    - $f" >&2
  done
  echo "" >&2
  echo "  프론트엔드 완료 순서:" >&2
  echo "    1. ./gradlew.bat test              # JUnit 테스트" >&2
  echo "    2. ./gradlew.bat bootRun           # 별도 터미널에서 서버 기동" >&2
  echo "    3. cd e2e && npx playwright test   # E2E 테스트 (필수!)" >&2
  echo "    4. git add <파일들>" >&2
  echo "    5. git commit -m \"<작업 내용>\"" >&2
  echo "" >&2
  echo "  ⚠️  git pre-commit hook이 E2E 리포트 타임스탬프를 검증합니다." >&2
  exit 2
fi

exit 0
