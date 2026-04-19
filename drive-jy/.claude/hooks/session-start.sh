#!/bin/bash
# SessionStart hook
# 직전 세션 스냅샷이 있으면 stdout 으로 출력하여 컨텍스트에 주입한다.
# 컨텍스트 압축으로 잃어버린 작업 흐름을 자동으로 복구한다.

SNAPSHOT=".claude/state/last-session.md"

if [ -f "$SNAPSHOT" ]; then
  echo "## 🔄 직전 세션에서 이어집니다"
  echo ""
  cat "$SNAPSHOT"
  echo ""
  echo "---"
  echo ""
fi

# 현재 진행 중인 계획서도 함께 (스냅샷과 별개로 살아있을 수 있음)
PLAN=".claude/state/current-plan.md"
if [ -f "$PLAN" ]; then
  echo "## 📋 현재 활성 계획서"
  echo ""
  cat "$PLAN"
  echo ""
  echo "---"
  echo ""
fi

exit 0
