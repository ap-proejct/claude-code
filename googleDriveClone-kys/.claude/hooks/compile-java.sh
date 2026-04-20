#!/usr/bin/env bash
# PostToolUse(Edit|Write): .java 파일 편집 후 gradle compileJava 실행해 즉시 피드백
# 에러 마지막 20줄만 출력. 훅 자체는 항상 성공(0) — 컴파일 실패도 exit 0로 알림만.

FILE=$(echo "$CLAUDE_TOOL_INPUT" | python3 -c "import sys, json; print(json.load(sys.stdin).get('file_path', ''))" 2>/dev/null)

if echo "$FILE" | grep -q '\.java$'; then
  cd /mnt/c/googleDriveClone/googleDrive && ./gradlew compileJava -q 2>&1 | tail -20
fi

exit 0
