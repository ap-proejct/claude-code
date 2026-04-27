#!/usr/bin/env bash
# PostToolUse(Edit|Write) — Claude 가 archived/*.md 를 편집했을 때 INDEX.jsonl 자동 재동기화.
# 사용자 IDE 편집은 훅 프로토콜상 감지 불가 (분석 커맨드 실행 시 resync 로 대응).
set -u

project_dir="${CLAUDE_PROJECT_DIR:-$(pwd)}"

file=$(python3 -c '
import sys, json
try:
    d = json.load(sys.stdin)
    print(d.get("tool_input", {}).get("file_path", ""))
except Exception:
    pass
' 2>/dev/null)

[[ -z "$file" ]] && exit 0

case "$file" in
  *.claude/plans/archived/*.md|.claude/plans/archived/*.md)
    # INDEX.jsonl 자체는 skip (이중 안전장치).
    case "$(basename "$file")" in
      INDEX.jsonl) exit 0 ;;
    esac
    bash "$project_dir/.claude/scripts/plan-resync.sh" 2>/dev/null || true
    ;;
esac

exit 0
