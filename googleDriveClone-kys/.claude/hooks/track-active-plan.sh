#!/usr/bin/env bash
# PostToolUse(Write|Edit) — 플랜 파일이 수정되면 state/current-plan.md 포인터 갱신.
# archived/ 하위는 제외 (이미 완료된 플랜).
set -u

project_dir="${CLAUDE_PROJECT_DIR:-$(pwd)}"
state_file="$project_dir/.claude/state/current-plan.md"
mkdir -p "$(dirname "$state_file")" 2>/dev/null || exit 0

# stdin JSON 에서 file_path 추출.
file=$(python3 -c '
import sys, json
try:
    d = json.load(sys.stdin)
    print(d.get("tool_input", {}).get("file_path", ""))
except Exception:
    pass
' 2>/dev/null)

[[ -z "$file" ]] && exit 0

# .claude/plans/*.md 만 대상 (archived 제외). 절대/상대 경로 모두 지원.
case "$file" in
  *.claude/plans/*.md|.claude/plans/*.md)
    case "$file" in
      *.claude/plans/archived/*|.claude/plans/archived/*) exit 0 ;;
    esac
    # 이미 같은 경로가 기록돼 있으면 noop.
    current=$(tr -d '[:space:]' < "$state_file" 2>/dev/null || true)
    if [[ "$current" != "$file" ]]; then
      echo "$file" > "$state_file"
    fi

    # ---- 프론트매터 자동 주입 ----
    # 플랜 파일에 YAML 프론트매터가 없으면 기본 스텁(task_type=feat, title=첫 H1, created=오늘) 프리펜드.
    # 이미 ---로 시작하면 멱등적으로 skip.
    PLAN_FILE="$file" python3 -c '
import os, re, datetime
p = os.environ["PLAN_FILE"]
if not os.path.isfile(p) or os.path.getsize(p) == 0:
    raise SystemExit(0)
with open(p, "r", encoding="utf-8") as f:
    content = f.read()
if content.startswith("---\n"):
    raise SystemExit(0)
m = re.search(r"^#\s+(.+)$", content, re.MULTILINE)
title = m.group(1).strip() if m else os.path.basename(p).replace(".md", "")
title = title.replace("\"", "\\\"")
today = datetime.date.today().isoformat()
fm = f"---\ntask_type: feat\ntitle: \"{title}\"\ncreated: {today}\n---\n"
with open(p, "w", encoding="utf-8") as f:
    f.write(fm + content)
' 2>/dev/null || true
    ;;
esac

exit 0
