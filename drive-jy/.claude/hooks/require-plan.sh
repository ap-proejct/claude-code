#!/bin/bash
# PreToolUse: Write|Edit
# 계획서 없으면 코드 수정 차단 (L4 — exit 2)

TOOL_INPUT="${CLAUDE_TOOL_INPUT:-}"
FILE=$(echo "$TOOL_INPUT" | python3 -c "import sys,json; d=json.load(sys.stdin); print(d.get('file_path',''))" 2>/dev/null || echo "")

# 파일 경로 정규화
FILE="${FILE//\\//}"

# 계획서 자체 수정 → 통과 (계획서 작성 허용)
if [[ "$FILE" == *".claude/state/current-plan.md" ]]; then
  exit 0
fi

# 메타파일 수정 → 통과
if [[ "$FILE" == *".claude/"* ]]; then exit 0; fi
if [[ "$FILE" == *.md ]]; then exit 0; fi
if [[ "$FILE" == *.json ]]; then exit 0; fi
if [[ "$FILE" == *.yaml || "$FILE" == *.yml ]]; then exit 0; fi
if [[ "$FILE" == *.sql ]]; then exit 0; fi
if [[ "$FILE" == *.properties ]]; then exit 0; fi
if [[ "$FILE" == *.xml ]]; then exit 0; fi

# 소스 파일 (*.kt, *.html, src/ 하위) → 계획서 검사
if [[ "$FILE" == *.kt || "$FILE" == *.html || "$FILE" == *"src/"* ]]; then
  PLAN=".claude/state/current-plan.md"

  if [ ! -f "$PLAN" ]; then
    echo "❌ 계획서 없음 — 코드 수정 차단" >&2
    echo "" >&2
    echo "  .claude/state/current-plan.md 에 작업 계획을 먼저 작성하세요." >&2
    echo "" >&2
    echo "  예시:" >&2
    echo "  ## 작업 목표" >&2
    echo "  - 무엇을 왜 변경하는지 설명" >&2
    echo "  ## 변경할 파일" >&2
    echo "  - src/main/kotlin/demo/drive/..." >&2
    echo "  ## 완료 조건" >&2
    echo "  - compileKotlin 통과, 테스트 통과, git commit" >&2
    exit 2
  fi

  SIZE=$(wc -c < "$PLAN" 2>/dev/null || echo 0)
  if [ "$SIZE" -lt 100 ]; then
    echo "❌ 계획서 내용 부족 (${SIZE} bytes) — 최소 100 bytes 이상 작성 필요" >&2
    echo "  .claude/state/current-plan.md 에 더 상세한 계획을 작성하세요." >&2
    exit 2
  fi
fi

exit 0
