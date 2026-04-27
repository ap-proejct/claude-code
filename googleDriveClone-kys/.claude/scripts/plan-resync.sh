#!/usr/bin/env bash
# archived/*.md 프론트매터를 전수 스캔해 INDEX.jsonl 재생성.
# md 가 source of truth, INDEX 는 자동 재생성되는 캐시.
# 분석 스크립트(harness-trend, antipattern-scan, similar-plans) 가 실행 초기에 호출.
set -u

project_dir="${CLAUDE_PROJECT_DIR:-$(pwd)}"
archived_dir="$project_dir/.claude/plans/archived"
index_file="$archived_dir/INDEX.jsonl"

[[ -d "$archived_dir" ]] || exit 0

ARCHIVED_DIR="$archived_dir" INDEX_FILE="$index_file" python3 -c '
import os, re, json, glob

archived_dir = os.environ["ARCHIVED_DIR"]
index_file = os.environ["INDEX_FILE"]

def parse_value(v):
    v = v.strip().strip(chr(34))
    if v == "null" or v == "":
        return None
    if v.lower() == "true": return True
    if v.lower() == "false": return False
    try: return int(v)
    except ValueError: pass
    return v

records = []
for p in sorted(glob.glob(os.path.join(archived_dir, "*.md"))):
    with open(p, "r", encoding="utf-8") as f:
        content = f.read()
    m = re.match(r"^---\n(.*?)\n---", content, re.DOTALL)
    if not m:
        continue
    fm = {}
    for line in m.group(1).splitlines():
        mm = re.match(r"^(\w+):\s*(.*)$", line.strip())
        if mm:
            fm[mm.group(1)] = parse_value(mm.group(2))

    rec = {
        "id": os.path.basename(p).replace(".md", ""),
        "archived": fm.get("archived"),
        "task_type": fm.get("task_type") or "chore",
        "score": fm.get("score"),                  # 사용자 수동 채점
        "score_auto": fm.get("score_auto"),        # harness-evaluator 자동 채점
        "score_external": fm.get("score_external"),# n8n + 외부 모델 채점 (Phase 2)
        "harness_sha": fm.get("harness_sha"),
        "title": fm.get("title") or "",
        "absorbed": bool(fm.get("absorbed", False)),
    }
    records.append(rec)

with open(index_file, "w", encoding="utf-8") as f:
    for r in records:
        f.write(json.dumps(r, ensure_ascii=False) + "\n")
'
