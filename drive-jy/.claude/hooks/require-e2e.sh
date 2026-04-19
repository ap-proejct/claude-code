#!/usr/bin/env bash
# require-e2e.sh — Controller 파일 생성/수정 시 E2E 스펙 파일 존재 여부 강제 확인
#
# 트리거: PostToolUse (Write/Edit) — *Controller.kt 패턴
# 입력: CLAUDE_TOOL_INPUT_FILE_PATH 환경변수로 편집된 파일 경로 전달

set -euo pipefail

# stdin에서 JSON 입력 읽기 (Claude Code hook 표준)
INPUT=$(cat)

# 편집된 파일 경로 추출
FILE_PATH=$(echo "$INPUT" | python3 -c "import sys,json; d=json.load(sys.stdin); print(d.get('tool_input',{}).get('file_path',''))" 2>/dev/null || echo "")

# Controller 파일이 아니면 무시
if [[ ! "$FILE_PATH" =~ Controller\.kt$ ]]; then
  exit 0
fi

# 도메인 추출: .../drive/{domain}/controller/XxxController.kt → {domain}
DOMAIN=$(echo "$FILE_PATH" | python3 -c "
import sys, re
m = re.search(r'demo/drive/([^/]+)/controller', sys.stdin.read())
print(m.group(1) if m else '')
" 2>/dev/null || echo "")

if [[ -z "$DOMAIN" ]]; then
  exit 0
fi

# auth 도메인은 user 도메인 컨트롤러이므로 별도 처리
# AuthController → auth spec, DriveController/FileController → file spec
CONTROLLER_BASENAME=$(basename "$FILE_PATH" .kt)
case "$CONTROLLER_BASENAME" in
  AuthController) E2E_DOMAIN="auth" ;;
  DriveController|FileController) E2E_DOMAIN="file" ;;
  *) E2E_DOMAIN="$DOMAIN" ;;
esac

# E2E 스펙 파일 경로
PROJECT_ROOT=$(echo "$FILE_PATH" | python3 -c "
import sys, re
m = re.search(r'(.+)/src/', sys.stdin.read())
print(m.group(1) if m else '')
" 2>/dev/null || echo "")
E2E_SPEC="$PROJECT_ROOT/e2e/specs/$E2E_DOMAIN/${E2E_DOMAIN}.spec.ts"

if [[ ! -f "$E2E_SPEC" ]]; then
  echo "⚠️  E2E 스펙 파일 없음: e2e/specs/$E2E_DOMAIN/${E2E_DOMAIN}.spec.ts" >&2
  echo "" >&2
  echo "   ${CONTROLLER_BASENAME}.kt 이 추가/수정되었지만 대응 E2E 테스트가 없습니다." >&2
  echo "   하네스 에이전트를 실행하거나 직접 파일을 생성하세요:" >&2
  echo "   e2e/specs/$E2E_DOMAIN/${E2E_DOMAIN}.spec.ts" >&2
  echo "" >&2
  # 경고만 출력, 차단(exit 1)하지 않음 — 작업 흐름 방해 최소화
fi

exit 0
