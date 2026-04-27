#!/usr/bin/env bash
# /task-archive 용 기계적 처리:
# 1. 활성 플랜 파일을 archived/ 로 이동
# 2. 프론트매터에 archived, harness_sha, absorbed:false, score:null 주입
# 3. state/current-plan.md 비움
# INDEX.jsonl 은 건들지 않음 (분석 스크립트가 실행 시 resync 로 재생성).
set -u

project_dir="${CLAUDE_PROJECT_DIR:-$(pwd)}"
state_file="$project_dir/.claude/state/current-plan.md"
archived_dir="$project_dir/.claude/plans/archived"

if [[ ! -s "$state_file" ]]; then
  echo "❌ 활성 플랜이 없습니다. Plan 모드로 플랜을 먼저 작성하세요." >&2
  exit 1
fi
plan_path=$(tr -d '[:space:]' < "$state_file")
if [[ ! -f "$plan_path" ]]; then
  echo "❌ 활성 플랜 파일이 존재하지 않습니다: $plan_path" >&2
  exit 1
fi

harness_sha=$(cd "$project_dir" && git log -1 --format=%h -- .claude/ 2>/dev/null || echo "nogit")

STATE_FILE="$state_file" PLAN_PATH="$plan_path" ARCHIVED_DIR="$archived_dir" \
  HARNESS_SHA="$harness_sha" \
  python3 - <<'PYEOF'
import os, re, datetime, sys

plan_path = os.environ["PLAN_PATH"]
archived_dir = os.environ["ARCHIVED_DIR"]
state_file = os.environ["STATE_FILE"]
harness_sha = os.environ["HARNESS_SHA"]

with open(plan_path, "r", encoding="utf-8") as f:
    content = f.read()

fm = {}
body = content
m = re.match(r"^---\n(.*?)\n---\n(.*)$", content, re.DOTALL)
if m:
    fm_raw, body = m.group(1), m.group(2)
    for line in fm_raw.splitlines():
        mm = re.match(r"^(\w+):\s*(.*)$", line.strip())
        if mm:
            fm[mm.group(1)] = mm.group(2).strip().strip('"')

today = datetime.date.today().isoformat()
task_type = fm.get("task_type", "chore")
title = fm.get("title") or os.path.basename(plan_path).replace(".md", "")
if "task_type" not in fm:
    print("⚠️  task_type 없음 → chore 로 fallback", file=sys.stderr)

fm.update({
    "task_type": task_type,
    "title": title,
    "created": fm.get("created", today),
    "archived": today,
    "score": None,           # 사용자 수동 채점 (1~5)
    "score_auto": None,      # harness-evaluator 자동 채점 (1~5)
    "score_external": None,  # n8n + 외부 모델 채점 (Phase 2, optional)
    "eval_notes": None,      # 평가 근거 한 줄
    "eval_at": None,         # 평가 시각 (ISO date)
    "harness_sha": harness_sha,
    "absorbed": False,
})

slug_raw = re.sub(r"[^\w가-힣\s-]", "", title).strip().lower()
slug = re.sub(r"[\s_]+", "-", slug_raw)[:40] or "untitled"
archived_name = f"{today.replace('-', '')}-{slug}.md"
archived_path = os.path.join(archived_dir, archived_name)

n = 2
while os.path.exists(archived_path):
    archived_path = os.path.join(archived_dir, archived_name.replace(".md", f"-{n}.md"))
    n += 1

def fm_to_yaml(d):
    out = []
    for k, v in d.items():
        if v is None:
            out.append(f"{k}: null")
        elif isinstance(v, bool):
            out.append(f"{k}: {str(v).lower()}")
        elif isinstance(v, int):
            out.append(f"{k}: {v}")
        else:
            vs = str(v).replace('"', '\\"')
            out.append(f'{k}: "{vs}"')
    return "\n".join(out)

new_content = f"---\n{fm_to_yaml(fm)}\n---\n{body.lstrip()}"
with open(archived_path, "w", encoding="utf-8") as f:
    f.write(new_content)

os.remove(plan_path)
with open(state_file, "w", encoding="utf-8") as f:
    f.write("")

print(f"✅ 아카이빙 완료: {archived_path}")
print(f"   유형: {task_type} | 하네스: {harness_sha} | 점수: (채점 대기)")
print(f"")
print(f"👉 다음 단계: harness-evaluator 서브에이전트를 호출해 score_auto 를 채우세요.")
print(f"   (사용자 채점 score 는 회고 검토 후 직접 1~5 로 수정)")
PYEOF
