#!/usr/bin/env bash
# /entropy-scan — 하네스 엔트로피 스캔 + 리포트 생성 (자동 삭제 없음).
# 출력: .claude/logs/entropy/YYYYMMDD.md
#
# 스캔 항목 (P0):
#   1. 30일 이상 absorbed=false 아카이브
#   2. 14일 이상 score:null + score_auto:null 아카이브 (채점 누락)
#   3. knowledge/ 미참조 파일
#   4. 30일간 호출 0회 훅
# (P1):
#   5. activity.jsonl 신호잡음비
#   6. confirmed 안티패턴 vs draft 비율
set -u

project_dir="${CLAUDE_PROJECT_DIR:-$(pwd)}"
out_dir="$project_dir/.claude/logs/entropy"
mkdir -p "$out_dir"

# 인덱스 최신화 (md → INDEX 재생성)
bash "$project_dir/.claude/scripts/plan-resync.sh"

today=$(date +%Y-%m-%d)
today_slug=$(date +%Y%m%d)
out_file="$out_dir/$today_slug.md"

PROJECT_DIR="$project_dir" OUT_FILE="$out_file" TODAY="$today" python3 - <<'PYEOF'
import os, re, json, glob, datetime, subprocess
from collections import defaultdict

project_dir = os.environ["PROJECT_DIR"]
out_file = os.environ["OUT_FILE"]
today = datetime.date.fromisoformat(os.environ["TODAY"])
cutoff_30 = today - datetime.timedelta(days=30)
cutoff_14 = today - datetime.timedelta(days=14)

archived_dir = os.path.join(project_dir, ".claude/plans/archived")
knowledge_dir = os.path.join(project_dir, ".claude/knowledge")
hooks_dir = os.path.join(project_dir, ".claude/hooks")
logs_dir = os.path.join(project_dir, ".claude/logs")

lines = [f"# Entropy Scan — {today.isoformat()}", ""]
lines.append("스캔 + 리포트 전용 (자동 삭제 없음). 사용자가 검토 후 직접 정리.")
lines.append("")

# ─── P0-1. 30일 이상 absorbed=false 아카이브 ───
lines.append("## P0-1. 30일 이상 미흡수 아카이브")
lines.append("")
lines.append("`absorbed:false` 인 채로 30일 이상 방치된 아카이브 — 안티패턴 후보거나 수동 흡수 누락.")
lines.append("")
old_unabsorbed = []
index_file = os.path.join(archived_dir, "INDEX.jsonl")
if os.path.isfile(index_file):
    with open(index_file, "r", encoding="utf-8") as f:
        for line in f:
            try: rec = json.loads(line)
            except: continue
            if rec.get("absorbed"): continue
            try:
                d = datetime.date.fromisoformat(rec.get("archived", ""))
            except: continue
            if d < cutoff_30:
                old_unabsorbed.append((d, rec))
old_unabsorbed.sort()
if not old_unabsorbed:
    lines.append("- ✅ 해당 없음")
else:
    for d, r in old_unabsorbed:
        age = (today - d).days
        lines.append(f"- `{r['id']}` ({age}일 전) — {r.get('title','')}")
lines.append("")

# ─── P0-2. 14일 이상 채점 누락 ───
lines.append("## P0-2. 14일 이상 채점 누락 아카이브")
lines.append("")
lines.append("`score`(사용자) + `score_auto`(evaluator) 둘 다 null 인 채로 14일 이상 — `/evaluate` 일괄 채점 권장.")
lines.append("")
unscored = []
if os.path.isfile(index_file):
    with open(index_file, "r", encoding="utf-8") as f:
        for line in f:
            try: rec = json.loads(line)
            except: continue
            if rec.get("absorbed"): continue
            if isinstance(rec.get("score"), int): continue
            if isinstance(rec.get("score_auto"), int): continue
            try:
                d = datetime.date.fromisoformat(rec.get("archived", ""))
            except: continue
            if d < cutoff_14:
                unscored.append((d, rec))
unscored.sort()
if not unscored:
    lines.append("- ✅ 해당 없음")
else:
    for d, r in unscored:
        age = (today - d).days
        lines.append(f"- `{r['id']}` ({age}일 전) — {r.get('title','')}")
    lines.append("")
    lines.append(f"**조치:** `/evaluate null` 로 일괄 자동 채점 ({len(unscored)}건)")
lines.append("")

# ─── P0-3. knowledge/ 미참조 파일 ───
lines.append("## P0-3. knowledge/ 미참조 파일")
lines.append("")
lines.append("`.claude/knowledge/**/*.md` 중 다른 md 파일에서 파일명/경로로 단 한 번도 언급되지 않은 파일.")
lines.append("(README/INDEX/.gitkeep 제외)")
lines.append("")
all_md = []
for root, _, files in os.walk(project_dir + "/.claude"):
    for fn in files:
        if fn.endswith(".md"):
            all_md.append(os.path.join(root, fn))

knowledge_files = []
for fp in glob.glob(os.path.join(knowledge_dir, "**/*.md"), recursive=True):
    name = os.path.basename(fp)
    if name in ("README.md", "INDEX.md") or name.startswith("."):
        continue
    knowledge_files.append(fp)

unreferenced = []
for kfp in knowledge_files:
    kname = os.path.basename(kfp)
    kstem = kname.replace(".md", "")
    found = False
    for other in all_md:
        if os.path.abspath(other) == os.path.abspath(kfp):
            continue
        try:
            with open(other, "r", encoding="utf-8") as f:
                txt = f.read()
            if kname in txt or kstem in txt:
                found = True
                break
        except: pass
    if not found:
        unreferenced.append(kfp)
if not unreferenced:
    lines.append("- ✅ 해당 없음")
else:
    for fp in unreferenced:
        rel = os.path.relpath(fp, project_dir)
        lines.append(f"- `{rel}`")
lines.append("")

# ─── P0-4. 30일간 호출 0회 훅 ───
lines.append("## P0-4. 30일간 호출 0회 훅 (settings.json 등록 기준)")
lines.append("")
lines.append("settings.json 에 등록된 훅 중 최근 30일 activity 로그에 흔적 없는 것.")
lines.append("(현재 log-activity.sh 가 PostToolUse 만 잡으므로 100% 정확하진 않음 — 보조 신호)")
lines.append("")
# 등록 훅 목록 추출
settings_paths = [
    os.path.join(project_dir, ".claude/settings.json"),
    os.path.join(project_dir, ".claude/settings.local.json"),
]
registered_hooks = set()
for sp in settings_paths:
    if not os.path.isfile(sp): continue
    try:
        with open(sp, "r", encoding="utf-8") as f:
            cfg = json.load(f)
    except: continue
    for ev_list in (cfg.get("hooks") or {}).values():
        for ev in ev_list:
            for h in ev.get("hooks", []):
                cmd = h.get("command", "")
                m = re.search(r"hooks/([\w.-]+\.sh)", cmd)
                if m:
                    registered_hooks.add(m.group(1))

# 활성 로그에서 호출 흔적 카운트 (최근 30일)
hook_calls = defaultdict(int)
for lf in glob.glob(os.path.join(logs_dir, "activity-*.jsonl")):
    try:
        with open(lf, "r", encoding="utf-8") as f:
            for line in f:
                try: r = json.loads(line)
                except: continue
                # log-activity 자체는 모든 도구를 잡으므로 hook 이름은 cmd 안에 있을 수 있음
                cmd = r.get("cmd", "") or ""
                for hk in registered_hooks:
                    if hk in cmd:
                        hook_calls[hk] += 1
    except: pass

unused_hooks = sorted(registered_hooks - set(hook_calls.keys()))
if not unused_hooks:
    lines.append(f"- ✅ 등록된 {len(registered_hooks)}개 훅 모두 최근 호출 흔적 있음")
else:
    lines.append(f"- 등록 {len(registered_hooks)}개 중 흔적 없음 {len(unused_hooks)}개:")
    for hk in unused_hooks:
        lines.append(f"  - `{hk}`")
lines.append("")

# ─── P1-5. activity 신호잡음비 ───
lines.append("## P1-5. activity 로그 신호잡음비")
lines.append("")
total_records = 0
empty_records = 0
for lf in glob.glob(os.path.join(logs_dir, "activity-*.jsonl")):
    try:
        with open(lf, "r", encoding="utf-8") as f:
            for line in f:
                try: r = json.loads(line)
                except: continue
                total_records += 1
                if not r.get("file") and not r.get("cmd"):
                    empty_records += 1
    except: pass
if total_records:
    pct = 100.0 * empty_records / total_records
    lines.append(f"- 전체 {total_records:,} / 빈(file·cmd 모두 없음) {empty_records:,} = **{pct:.1f}%** 노이즈")
    if pct > 50:
        lines.append(f"- ⚠️ 노이즈 비율 50% 초과 — log-activity.sh 의 NOISY_NO_FILE 필터 보강 검토")
else:
    lines.append("- 로그 없음")
lines.append("")

# ─── P1-6. 안티패턴 draft vs confirmed ───
lines.append("## P1-6. 안티패턴 draft / confirmed 비율")
lines.append("")
ap_dir = os.path.join(knowledge_dir, "antipatterns")
draft_n = confirmed_n = 0
if os.path.isdir(ap_dir):
    for fp in glob.glob(os.path.join(ap_dir, "*.md")):
        try:
            with open(fp, "r", encoding="utf-8") as f:
                txt = f.read()
            if re.search(r"^status:\s*draft", txt, re.M):
                draft_n += 1
            elif re.search(r"^status:\s*confirmed", txt, re.M):
                confirmed_n += 1
        except: pass
lines.append(f"- draft: {draft_n} / confirmed: {confirmed_n}")
if draft_n > 3:
    lines.append(f"- ⚠️ draft {draft_n}건 누적 — `/absorb` 로 확정 또는 삭제 필요")
lines.append("")

# ─── 요약 ───
lines.append("## 요약 액션 아이템")
lines.append("")
todo = []
if old_unabsorbed:
    todo.append(f"- [ ] 30일 이상 미흡수 아카이브 {len(old_unabsorbed)}건 검토 → `/antipattern-scan` 또는 수동 absorbed:true 플래그")
if unscored:
    todo.append(f"- [ ] 14일 이상 미채점 {len(unscored)}건 → `/evaluate null` 일괄 자동 채점")
if unreferenced:
    todo.append(f"- [ ] knowledge 미참조 파일 {len(unreferenced)}건 → 다른 md 에서 링크 추가 또는 삭제")
if unused_hooks:
    todo.append(f"- [ ] 미사용 훅 {len(unused_hooks)}개 → settings.json 에서 제거 검토")
if not todo:
    todo.append("- ✅ 액션 아이템 없음 — 하네스 엔트로피 양호")
lines.extend(todo)

with open(out_file, "w", encoding="utf-8") as f:
    f.write("\n".join(lines) + "\n")

print(f"✅ 엔트로피 스캔 완료: {out_file}")
print(f"   미흡수 {len(old_unabsorbed)} / 미채점 {len(unscored)} / 미참조 {len(unreferenced)} / 미사용훅 {len(unused_hooks)}")
PYEOF
