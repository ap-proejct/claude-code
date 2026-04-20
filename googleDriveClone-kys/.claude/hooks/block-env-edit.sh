#!/usr/bin/env bash
# PreToolUse(Edit|Write): .env 파일 직접 편집 차단
# CLAUDE_TOOL_INPUT 환경변수로 {"file_path": "..."} JSON이 전달된다.

FILE=$(echo "$CLAUDE_TOOL_INPUT" | python3 -c "import sys, json; print(json.load(sys.stdin).get('file_path', ''))" 2>/dev/null)

if echo "$FILE" | grep -qE '\.env$|\.env\.'; then
  echo 'BLOCKED: .env 파일은 Claude가 직접 편집하지 않습니다. 민감 정보 보호를 위해 직접 수정해주세요.'
  exit 1
fi

exit 0
