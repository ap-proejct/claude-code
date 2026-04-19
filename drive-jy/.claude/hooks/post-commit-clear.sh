#!/bin/bash
# PostToolUse: Bash
# git commit 성공 후 계획서를 아카이브로 이동

TOOL_INPUT="${CLAUDE_TOOL_INPUT:-}"
COMMAND=$(echo "$TOOL_INPUT" | python3 -c "import sys,json; d=json.load(sys.stdin); print(d.get('command',''))" 2>/dev/null || echo "")
EXIT_CODE="${CLAUDE_TOOL_RESULT_EXIT_CODE:-1}"

# git commit 명령이 아니면 통과
if [[ "$COMMAND" != *"git commit"* ]]; then
  exit 0
fi

# 커밋 실패 → 아카이브 안 함
if [ "$EXIT_CODE" != "0" ]; then
  exit 0
fi

PLAN=".claude/state/current-plan.md"
ARCHIVE_DIR=".claude/plans/archived"

# 계획서가 없거나 비어있으면 통과
if [ ! -f "$PLAN" ] || [ ! -s "$PLAN" ]; then
  exit 0
fi

# 아카이브 디렉토리 확인
mkdir -p "$ARCHIVE_DIR"

# 타임스탬프로 아카이브
TIMESTAMP=$(date +%Y%m%d-%H%M%S)
ARCHIVED="$ARCHIVE_DIR/$TIMESTAMP.md"
mv "$PLAN" "$ARCHIVED"

echo "📦 계획서 아카이브 완료: $ARCHIVED"
echo "   다음 작업은 .claude/state/current-plan.md 를 새로 작성하세요."

exit 0
