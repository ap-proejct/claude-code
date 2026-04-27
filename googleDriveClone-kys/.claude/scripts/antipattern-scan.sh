#!/usr/bin/env bash
# /antipattern-scan — 점수 ≤2 플랜 3건 이상 공통 키워드를 knowledge/antipatterns/ 초안으로 저장.
# 실행 전 plan-resync 로 md → INDEX 재생성.
set -u

project_dir="${CLAUDE_PROJECT_DIR:-$(pwd)}"
bash "$project_dir/.claude/scripts/plan-resync.sh"
index_file="$project_dir/.claude/plans/archived/INDEX.jsonl"
archived_dir="$project_dir/.claude/plans/archived"
out_dir="$project_dir/.claude/knowledge/antipatterns"
mkdir -p "$out_dir"

if [[ ! -s "$index_file" ]]; then
  echo "아직 아카이빙된 플랜이 없습니다." >&2
  exit 0
fi

INDEX_FILE="$index_file" ARCHIVED_DIR="$archived_dir" OUT_DIR="$out_dir" \
  python3 - <<'PYEOF'
import os, re, json, datetime
from collections import Counter

index_file = os.environ["INDEX_FILE"]
archived_dir = os.environ["ARCHIVED_DIR"]
out_dir = os.environ["OUT_DIR"]

# 한국어·영어 stop words 최소 집합
STOP = set("""
the a an and or of to in on for is are was were be been
기능 작업 플랜 계획 파일 내용 부분 시점 시간 경우 이를 위한 이런 그런 저런
위의 아래 다음 이후 이전 때문 하지만 그러나 그리고 또한 또는
""".split())

# 1. 점수 ≤2 + absorbed:false 필터
# score 우선, 없으면 score_auto 사용 (사용자 수동 채점이 자동 채점보다 신뢰도 높음)
low = []
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
        effective = rec.get("score") if isinstance(rec.get("score"), int) else rec.get("score_auto")
        if isinstance(effective, int) and effective <= 2:
            rec["_effective_score"] = effective
            rec["_score_source"] = "manual" if isinstance(rec.get("score"), int) else "auto"
            low.append(rec)

if len(low) < 3:
    print(f"점수 ≤2 플랜이 {len(low)}건 (3건 이상 필요). 아직 안티패턴 추출은 이릅니다.")
    raise SystemExit(0)

# 2. 각 플랜 본문+note 에서 토큰 추출
def tokens(text):
    # 한글·영숫자 2자 이상 단어
    for w in re.findall(r"[가-힣]{2,}|[A-Za-z]{3,}", text):
        w = w.lower()
        if w in STOP:
            continue
        yield w

# 플랜별로 set 화 (같은 플랜 내 중복은 1로 계산 → IDF 유사 효과)
per_plan_words = []
plan_notes = []
for rec in low:
    pid = rec.get("id", "")
    path = os.path.join(archived_dir, pid + ".md")
    content = ""
    if os.path.isfile(path):
        with open(path, "r", encoding="utf-8") as f:
            # 본문 첫 100줄만
            content = "".join(f.readlines()[:100])
    text = (rec.get("title", "") + " " + content).lower()
    # note 는 frontmatter 에 있으므로 content 에 포함됨
    per_plan_words.append(set(tokens(text)))
    plan_notes.append((pid, rec.get("title", ""), rec["_effective_score"], rec["_score_source"]))

# 3. "3건 이상 공통" 키워드 집계
doc_freq = Counter()
for ws in per_plan_words:
    for w in ws:
        doc_freq[w] += 1

common = [(w, c) for w, c in doc_freq.items() if c >= 3]
common.sort(key=lambda x: -x[1])

if not common:
    print(f"점수 ≤2 플랜 {len(low)}건을 분석했지만 3건 이상 공통 키워드 없음.")
    raise SystemExit(0)

# 4. 상위 5개 키워드에 대해 초안 생성
today = datetime.date.today().isoformat()
today_slug = today.replace("-", "")
created_count = 0

for kw, freq in common[:5]:
    safe_kw = re.sub(r"[^\w가-힣]", "", kw)[:20]
    out_path = os.path.join(out_dir, f"{today_slug}-{safe_kw}.md")
    if os.path.exists(out_path):
        continue  # 같은 날짜·키워드 초안 중복 방지

    # 해당 키워드 포함한 플랜 ID 수집
    matching_plans = [
        plan_notes[i] for i, ws in enumerate(per_plan_words) if kw in ws
    ]
    source_ids = [p[0] for p in matching_plans]

    lines = [
        "---",
        f'pattern: "{kw}"',
        f"source_plans: [{', '.join(repr(s) for s in source_ids)}]",
        f"detected: {today}",
        "status: draft",
        "---",
        "",
        f"# 안티패턴 초안: '{kw}' (수동 제목 필요)",
        "",
        f"점수 ≤2 플랜 {freq}건에서 반복 등장한 키워드입니다. 실제 공통 실패 요인인지 사용자가 검토·요약해야 합니다.",
        "",
        "## 원본 플랜",
        "",
    ]
    for pid, title, score, source in matching_plans:
        src_label = "수동" if source == "manual" else "자동"
        lines.append(f"- **[{score}점·{src_label}]** `{pid}` — {title}")

    lines += [
        "",
        "## TODO (사용자 작성)",
        "",
        "- 이 키워드가 실제 공통 실패 요인인가? (예/아니오)",
        "- 구체 실패 패턴 1~2줄 요약",
        "- 회피 방법: ",
        "",
        "> 확정 시 Phase 3의 `/absorb` 커맨드로 원본 플랜들에 `absorbed:true` 를 설정해 트렌드에서 제외.",
    ]

    with open(out_path, "w", encoding="utf-8") as f:
        f.write("\n".join(lines) + "\n")
    print(f"📝 초안 생성: {out_path} (출현 {freq}건)")
    created_count += 1

if created_count == 0:
    print(f"모든 상위 키워드 초안이 이미 존재합니다 ({today}). 기존 초안을 확인하세요.")
else:
    print(f"\n총 {created_count}개 초안 생성. 내용 검토 후 수동 확정 필요.")
PYEOF
