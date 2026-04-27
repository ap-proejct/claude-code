#!/usr/bin/env bash
# PostToolUse(Edit|Write) — 방금 수정된 md 파일이 200줄 초과 시 경고.
# 블로킹이 아닌 경고 전용 (기존 위반 파일 고려한 점진 수정 방침).
#
# Claude Code 공식 훅 프로토콜: stdin 으로 JSON 전달
#   { hook_event_name, tool_name, tool_input: {file_path, ...}, tool_response, ... }
set -u

payload=$(cat 2>/dev/null || true)

# stdin JSON → python 으로 file_path 추출 (repo 내 기존 훅과 동일한 파싱 기법).
file=$(printf '%s' "$payload" | python3 -c "
import sys, json
try:
    d = json.load(sys.stdin)
    print(d.get('tool_input', {}).get('file_path', ''))
except Exception:
    pass
" 2>/dev/null)

# env 백업 (수동 호출·테스트 시 대비).
[[ -z "${file:-}" ]] && file="${CLAUDE_TOOL_INPUT_file_path:-}"

[[ -z "$file" ]] && exit 0
[[ "$file" == *.md ]] || exit 0
[[ -f "$file" ]] || exit 0

# 면제 목록: 장문이 불가피한 문서.
case "$(basename "$file")" in
  PLAN.md|CHANGELOG.md|README.md) exit 0 ;;
esac

lines=$(wc -l < "$file")
if (( lines > 200 )); then
  echo "::warning:: $file is $lines lines (>200). Consider splitting into knowledge/<topic>/ subfiles." >&2
fi

exit 0
