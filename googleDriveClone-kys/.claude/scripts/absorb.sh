#!/usr/bin/env bash
# /absorb <안티패턴 md> — draft 안티패턴을 confirmed 로 전환하고 소스 플랜들의 absorbed:true 로 갱신.
# 실행 마지막에 plan-resync 로 INDEX 재생성.
set -u

project_dir="${CLAUDE_PROJECT_DIR:-$(pwd)}"
antip_dir="$project_dir/.claude/knowledge/antipatterns"
archived_dir="$project_dir/.claude/plans/archived"

arg="${1:-}"

if [[ -z "$arg" ]]; then
  echo "사용법: /absorb <안티패턴 md 경로 또는 파일명>"
  echo ""
  if [[ -d "$antip_dir" ]]; then
    echo "현재 draft 상태 안티패턴:"
    found=0
    for f in "$antip_dir"/*.md; do
      [[ -f "$f" ]] || continue
      if grep -q '^status: draft' "$f" 2>/dev/null; then
        echo "  - $(basename "$f")"
        found=1
      fi
    done
    [[ "$found" == 0 ]] && echo "  (없음)"
  else
    echo "아직 안티패턴 디렉토리가 없습니다. /antipattern-scan 먼저 실행하세요."
  fi
  exit 1
fi

# 경로 해석: 절대/상대 경로 → 그대로, 파일명만 → antip_dir 결합
if [[ -f "$arg" ]]; then
  target="$arg"
elif [[ -f "$antip_dir/$arg" ]]; then
  target="$antip_dir/$arg"
elif [[ -f "$antip_dir/${arg}.md" ]]; then
  target="$antip_dir/${arg}.md"
else
  echo "파일을 찾을 수 없습니다: $arg" >&2
  exit 1
fi

TARGET="$target" ARCHIVED_DIR="$archived_dir" python3 - <<'PYEOF'
import os, re, sys, datetime

target = os.environ["TARGET"]
archived_dir = os.environ["ARCHIVED_DIR"]

with open(target, "r", encoding="utf-8") as f:
    content = f.read()

m = re.match(r"^---\n(.*?)\n---\n", content, re.DOTALL)
if not m:
    print(f"프론트매터를 찾을 수 없습니다: {target}", file=sys.stderr)
    sys.exit(1)

fm_text = m.group(1)
body = content[m.end():]

# status 확인
status = ""
for line in fm_text.splitlines():
    mm = re.match(r"^status:\s*(.*)$", line.strip())
    if mm:
        status = mm.group(1).strip().strip('"').strip("'")
        break

if status == "confirmed":
    print(f"이미 confirmed 상태입니다: {os.path.basename(target)}")
    sys.exit(0)

# source_plans 파싱: ['id1', 'id2', ...] 또는 ["id1", "id2"]
sp_match = re.search(r"^source_plans:\s*\[(.*?)\]\s*$", fm_text, re.MULTILINE)
if not sp_match:
    print("source_plans 필드를 찾을 수 없습니다.", file=sys.stderr)
    sys.exit(1)

raw = sp_match.group(1)
ids = re.findall(r"['\"]([^'\"]+)['\"]", raw)
if not ids:
    print("source_plans 가 비어 있습니다.", file=sys.stderr)
    sys.exit(1)

today = datetime.date.today().isoformat()

# 안티패턴 파일 업데이트: status 치환 + confirmed_at 추가
new_fm = []
status_seen = False
confirmed_at_seen = False
for line in fm_text.splitlines():
    if re.match(r"^status:\s*", line):
        new_fm.append("status: confirmed")
        status_seen = True
    elif re.match(r"^confirmed_at:\s*", line):
        new_fm.append(f"confirmed_at: {today}")
        confirmed_at_seen = True
    else:
        new_fm.append(line)
if not status_seen:
    new_fm.append("status: confirmed")
if not confirmed_at_seen:
    new_fm.append(f"confirmed_at: {today}")

new_content = "---\n" + "\n".join(new_fm) + "\n---\n" + body
with open(target, "w", encoding="utf-8") as f:
    f.write(new_content)
print(f"✅ 안티패턴 확정: {os.path.basename(target)} (status → confirmed)")

# 각 소스 플랜 absorbed 갱신
updated = 0
skipped = 0
missing = 0
for pid in ids:
    path = os.path.join(archived_dir, pid + ".md")
    if not os.path.isfile(path):
        print(f"  ⚠ 원본 플랜 누락: {pid}", file=sys.stderr)
        missing += 1
        continue

    with open(path, "r", encoding="utf-8") as f:
        pc = f.read()
    pm = re.match(r"^---\n(.*?)\n---\n", pc, re.DOTALL)
    if not pm:
        print(f"  ⚠ 프론트매터 없음: {pid}", file=sys.stderr)
        missing += 1
        continue

    p_fm = pm.group(1)
    p_body = pc[pm.end():]

    absorbed_val = None
    new_p_fm_lines = []
    for line in p_fm.splitlines():
        amt = re.match(r"^absorbed:\s*(.*)$", line.strip())
        if amt:
            absorbed_val = amt.group(1).strip().lower()
            new_p_fm_lines.append("absorbed: true")
        else:
            new_p_fm_lines.append(line)

    if absorbed_val == "true":
        print(f"  - {pid}: 이미 absorbed:true (skip)")
        skipped += 1
        continue

    if absorbed_val is None:
        new_p_fm_lines.append("absorbed: true")

    new_pc = "---\n" + "\n".join(new_p_fm_lines) + "\n---\n" + p_body
    with open(path, "w", encoding="utf-8") as f:
        f.write(new_pc)
    print(f"  ✓ {pid}: absorbed → true")
    updated += 1

print(f"\n요약: 갱신 {updated}건, 스킵 {skipped}건, 누락 {missing}건")
PYEOF

rc=$?
[[ $rc -ne 0 ]] && exit $rc

# INDEX 재동기화 (bash sed 편집은 Layer A 훅 대상 아님)
bash "$project_dir/.claude/scripts/plan-resync.sh" 2>/dev/null || true
echo "INDEX 재동기화 완료."
