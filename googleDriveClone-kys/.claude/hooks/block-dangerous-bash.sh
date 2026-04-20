#!/usr/bin/env bash
# PreToolUse(Bash): 위험 명령 차단 (rm -rf /, git push --force main/master, DROP DATABASE/TABLE)

CMD=$(echo "$CLAUDE_TOOL_INPUT" | python3 -c "import sys, json; print(json.load(sys.stdin).get('command', ''))" 2>/dev/null)

# 루트/홈 대상 rm -rf
if echo "$CMD" | grep -qE '(^|[; &|])rm[[:space:]]+(-[a-zA-Z]*[rfR][a-zA-Z]*[[:space:]]+)+(/$|/\*|/[[:space:]]|~$|~/$)'; then
  echo 'BLOCKED: 루트/홈 디렉터리 대상의 rm -rf는 차단됩니다.'
  exit 1
fi

# main/master force push
if echo "$CMD" | grep -qE 'git[[:space:]]+push[[:space:]]+.*--force.*(main|master)'; then
  echo 'BLOCKED: main/master 브랜치 force push는 차단됩니다.'
  exit 1
fi

# 중요 DB/테이블 DROP
if echo "$CMD" | grep -qiE 'DROP[[:space:]]+(DATABASE|TABLE[[:space:]]+users|TABLE[[:space:]]+files)'; then
  echo 'BLOCKED: 중요 DB/테이블 DROP은 차단됩니다. 수동으로 실행해주세요.'
  exit 1
fi

exit 0
