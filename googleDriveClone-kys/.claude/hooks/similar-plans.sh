#!/usr/bin/env bash
# SessionStart — 현재 활성 플랜의 task_type 과 동일한 과거 고점(≥4) 플랜 상위 3건을 stdout 으로 주입.
set -u

project_dir="${CLAUDE_PROJECT_DIR:-$(pwd)}"
state_file="$project_dir/.claude/state/current-plan.md"
index_file="$project_dir/.claude/plans/archived/INDEX.jsonl"

# 활성 플랜 없으면 조용히 종료.
[[ -s "$state_file" ]] || exit 0

# md → INDEX 재생성 (아카이브 md 파일 기준 최신 상태 반영).
bash "$project_dir/.claude/scripts/plan-resync.sh" 2>/dev/null || true
[[ -s "$index_file" ]] || exit 0

plan_path=$(tr -d '[:space:]' < "$state_file")
[[ -f "$plan_path" ]] || exit 0

PROJECT_DIR="$project_dir" PLAN_PATH="$plan_path" INDEX_FILE="$index_file" \
  python3 - <<'PYEOF'
import os, re, json

plan_path = os.environ["PLAN_PATH"]
index_file = os.environ["INDEX_FILE"]

# 현재 플랜의 task_type 읽기
task_type = ""
with open(plan_path, "r", encoding="utf-8") as f:
    content = f.read()
m = re.match(r"^---\n(.*?)\n---", content, re.DOTALL)
if m:
    for line in m.group(1).splitlines():
        mm = re.match(r"^task_type:\s*(.*)$", line.strip())
        if mm:
            task_type = mm.group(1).strip().strip('"')
            break

if not task_type:
    raise SystemExit(0)

# 동일 task_type + score≥4 + absorbed:false
candidates = []
with open(index_file, "r", encoding="utf-8") as f:
    for line in f:
        line = line.strip()
        if not line:
            continue
        try:
            rec = json.loads(line)
        except Exception:
            continue
        if rec.get("absorbed"):
            continue
        if rec.get("task_type") != task_type:
            continue
        if not isinstance(rec.get("score"), int) or rec["score"] < 4:
            continue
        candidates.append(rec)

# 점수 내림차순, 같으면 최신순
candidates.sort(key=lambda r: (-r.get("score", 0), r.get("archived", "")), reverse=False)
# 위 정렬은 점수 DESC + archived ASC. 우리는 점수 DESC + archived DESC 원함.
candidates.sort(key=lambda r: (-r.get("score", 0), -int(r.get("archived", "0").replace("-", "") or 0)))
top = candidates[:3]

if not top:
    raise SystemExit(0)

print(f"## 🎯 유사 작업 과거 고점 플랜 (task_type={task_type})")
print()
for rec in top:
    print(f"- [{rec['score']}점] plans/archived/{rec['id']}.md — \"{rec.get('title', '')}\"")
print()
print("위 플랜들의 Context·검증 섹션 구조를 참고하면 이번 작업의 완성도를 높일 수 있습니다.")
print("---")
PYEOF
