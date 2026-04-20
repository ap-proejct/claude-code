#!/usr/bin/env bash
# PostToolUse(Edit|Write): frontend/ 내 .jsx/.js 편집 시 eslint 실행
# --no-install 로 설치 안된 경우 조용히 스킵. 결과 마지막 15줄만 출력.

FILE=$(echo "$CLAUDE_TOOL_INPUT" | python3 -c "import sys, json; print(json.load(sys.stdin).get('file_path', ''))" 2>/dev/null)

if echo "$FILE" | grep -qE '\.(jsx|js)$' && echo "$FILE" | grep -q '/frontend/'; then
  cd /mnt/c/googleDriveClone/frontend && npx --no-install eslint "$FILE" 2>&1 | tail -15
fi

exit 0
