#!/usr/bin/env bash
# /harness-trend — 최근 30건 · 90일 슬라이딩 윈도우로 하네스 SHA별 평균 점수 출력.
# 실행 전 plan-resync 로 md → INDEX 재생성 (md 가 source of truth).
set -u

project_dir="${CLAUDE_PROJECT_DIR:-$(pwd)}"
bash "$project_dir/.claude/scripts/plan-resync.sh"
index_file="$project_dir/.claude/plans/archived/INDEX.jsonl"

if [[ ! -s "$index_file" ]]; then
  echo "아직 아카이빙된 플랜이 없습니다. /task-archive 로 첫 플랜을 완료해보세요." >&2
  exit 0
fi

PROJECT_DIR="$project_dir" INDEX_FILE="$index_file" python3 - <<'PYEOF'
import os, json, datetime, subprocess
from collections import defaultdict, Counter

index_file = os.environ["INDEX_FILE"]
project_dir = os.environ["PROJECT_DIR"]
today = datetime.date.today()
cutoff = today - datetime.timedelta(days=90)

records = []
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
        # 점수 우선순위: score (사용자) > score_auto (evaluator) > 제외
        # 둘 다 null 이면 집계에서 제외
        effective = rec.get("score") if isinstance(rec.get("score"), int) else rec.get("score_auto")
        if not isinstance(effective, int):
            continue
        rec["_effective_score"] = effective
        rec["_score_source"] = "manual" if isinstance(rec.get("score"), int) else "auto"
        try:
            d = datetime.date.fromisoformat(rec.get("archived", ""))
        except Exception:
            continue
        if d < cutoff:
            continue
        records.append((d, rec))

# 최근 30건으로 컷
records.sort(key=lambda x: x[0], reverse=True)
records = records[:30]

if not records:
    print("최근 90일 내 채점 완료된 플랜이 없습니다.")
    print("(score 또는 score_auto 중 하나라도 1~5 정수면 집계 대상)")
    print("→ /evaluate 로 미채점 플랜 일괄 자동 채점 가능")
    raise SystemExit(0)

# 채점 소스별 카운트
manual_n = sum(1 for _, r in records if r["_score_source"] == "manual")
auto_n = sum(1 for _, r in records if r["_score_source"] == "auto")

# 현재 하네스 SHA
try:
    current_sha = subprocess.check_output(
        ["git", "log", "-1", "--format=%h", "--", ".claude/"],
        cwd=project_dir, stderr=subprocess.DEVNULL, text=True,
    ).strip()
except Exception:
    current_sha = ""

# 그룹핑
groups = defaultdict(list)
for _, rec in records:
    groups[rec.get("harness_sha", "unknown")].append(rec)

print(f"=== Harness Trend (최근 {len(records)}건 · 90일) ===")
print(f"채점 소스: 수동 {manual_n}건 / 자동 {auto_n}건")
print()
print(f"{'harness_sha':<14} {'커밋 메시지':<35} {'건수':<5} {'평균':<6} task_type 분포")
print("-" * 90)

# 그룹별 최신 순으로 출력 (해당 SHA의 첫 기록 기준)
group_order = sorted(groups.items(), key=lambda kv: max(datetime.date.fromisoformat(r["archived"]) for r in kv[1]), reverse=True)

for sha, recs in group_order:
    scores = [r["_effective_score"] for r in recs]
    avg = sum(scores) / len(scores) if scores else 0
    types = Counter(r.get("task_type", "?") for r in recs)
    type_str = " ".join(f"{k}:{v}" for k, v in types.most_common())
    try:
        msg = subprocess.check_output(
            ["git", "show", "-s", "--format=%s", sha],
            cwd=project_dir, stderr=subprocess.DEVNULL, text=True,
        ).strip()[:33]
    except Exception:
        msg = "(커밋 정보 없음)"
    sha_label = f"{sha} (현재)" if sha == current_sha else sha
    warn = " ⚠️" if len(recs) < 5 else ""
    print(f"{sha_label:<14} {msg:<35} {len(recs):<5} {avg:.1f}{warn:<5} {type_str}")

print()
if any(len(recs) < 5 for recs in groups.values()):
    print("⚠️  = 표본 5건 미만 — 평균이 불안정하니 조기 결론 주의.")

# 최근/이전 두 그룹 비교 (있으면)
if len(group_order) >= 2:
    recent_sha, recent_recs = group_order[0]
    prev_sha, prev_recs = group_order[1]
    r_avg = sum(r["_effective_score"] for r in recent_recs) / len(recent_recs)
    p_avg = sum(r["_effective_score"] for r in prev_recs) / len(prev_recs)
    delta = r_avg - p_avg
    sign = "+" if delta >= 0 else ""
    print(f"변화: {prev_sha} → {recent_sha} = {sign}{delta:.2f}점 (표본 {len(recent_recs)} vs {len(prev_recs)})")
PYEOF
