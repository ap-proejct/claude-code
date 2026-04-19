#!/bin/bash
# PreCompact hook
# 컨텍스트 압축 직전, 핵심 작업 상태를 .claude/state/last-session.md 에 저장한다.
# 다음 세션에서 SessionStart hook 이 이 파일을 자동으로 컨텍스트에 주입한다.

SNAPSHOT=".claude/state/last-session.md"
mkdir -p .claude/state

{
  echo "# 직전 세션 스냅샷"
  echo ""
  echo "_$(date '+%Y-%m-%d %H:%M:%S') 컨텍스트 압축 시점에 자동 저장됨_"
  echo ""

  # 1. 현재 계획서 (있으면)
  if [ -f .claude/state/current-plan.md ]; then
    echo "## 진행 중이던 계획"
    echo ""
    cat .claude/state/current-plan.md
    echo ""
  else
    echo "## 진행 중이던 계획"
    echo ""
    echo "(계획서 없음 — 다음 작업 시 새로 작성 필요)"
    echo ""
  fi

  # 2. git 상태 (커밋 미완료 변경)
  if [ -d .git ]; then
    echo "## git 상태 (미커밋 변경)"
    echo ""
    echo '```'
    git status --short 2>/dev/null | head -30
    echo '```'
    echo ""

    echo "## 최근 커밋 5개"
    echo ""
    echo '```'
    git log --oneline -5 2>/dev/null
    echo '```'
    echo ""
  fi

  # 3. 최근 수정 파일 (10분 내)
  echo "## 최근 수정된 .kt 파일 (10분 이내)"
  echo ""
  echo '```'
  find src -name "*.kt" -mmin -10 2>/dev/null | head -20
  echo '```'
  echo ""

  echo "---"
  echo ""
  echo "**다음 세션 안내**: 이 스냅샷을 참고해 작업을 이어가세요. 필요 시 \`.claude/state/current-plan.md\` 를 새로 작성하세요."

} > "$SNAPSHOT"

echo "📸 직전 세션 스냅샷 저장: $SNAPSHOT"
exit 0
