#!/bin/bash
# PostToolUse: Write|Edit
# .kt 파일 변경 시 자동 컴파일 (L3 — 차단 없음, 피드백만)

TOOL_INPUT="${CLAUDE_TOOL_INPUT:-}"
FILE=$(echo "$TOOL_INPUT" | python3 -c "import sys,json; d=json.load(sys.stdin); print(d.get('file_path',''))" 2>/dev/null || echo "")

FILE="${FILE//\\//}"

# .kt 파일이 아니면 통과
[[ "$FILE" != *.kt ]] && exit 0

# Windows 환경 감지
if [[ "$OSTYPE" == "msys" || "$OSTYPE" == "cygwin" || -f "./gradlew.bat" ]]; then
  GRADLE="./gradlew.bat"
else
  GRADLE="./gradlew"
fi

OUTPUT=$($GRADLE compileKotlin --quiet 2>&1)
EXIT_CODE=$?

if [ $EXIT_CODE -ne 0 ]; then
  echo "❌ compileKotlin 실패 — 즉시 수정 필요" >&2
  echo "" >&2
  echo "$OUTPUT" | tail -30 >&2
  # exit 0 유지 — 차단은 안 함, 다음 턴에서 Claude 가 수정하도록 압박
  exit 0
else
  echo "✅ compileKotlin 통과"
fi

exit 0
