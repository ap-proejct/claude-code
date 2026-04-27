---
task_type: "test"
title: "Playwright MCP 스크린샷 캡처 동작 검증"
created: "2026-04-24"
archived: "2026-04-24"
score: null
harness_sha: "5810a1b"
absorbed: false
score_auto: 4
eval_notes: "검증 목적 플랜으로 범위가 작지만 회고-실제 파일 정합성 완벽(PNG 1280x720, 24887B 일치), 리스크 해소·범위 축소·훅 우회 등 아쉬움도 정직하게 기록."
eval_at: "2026-04-27"
---
# Playwright MCP 스크린샷 캡처 동작 검증

## Context

직전 턴에서 `e2e-flows.md` 에 "증거 캡처(수동 트리거)" 섹션을 추가하며 `browser_take_screenshot(filename)` 을 이슈 발견 시 즉시 호출하는 컨벤션을 정했다. 문서만 쓰고 실제 동작은 한 번도 돌려보지 않았으므로, 실제 환경에서 다음을 검증해야 한다.

- `browser_take_screenshot` 이 `filename` 인자를 받아 지정 경로에 PNG 를 저장하는지
- 저장 경로가 `.claude/logs/e2e/<basename>/` 처럼 프로젝트 루트 하위로 지정 가능한지 (이전에 `browser_file_upload` 가 `/tmp` 를 거부했던 경험 반복 방지)
- 실제 파일이 생성되고 유효한 PNG 인지

검증만 목적이므로 Flow 전체를 다시 돌리지 않고 **로그인 화면 1컷** 만 촬영.

## 작업 단계

1. 백엔드·프론트엔드 WSL 기동 (이미 있는 `/tmp/start-backend.sh` + `npm run dev`)
2. `http://localhost:5173/login` 이동, 로그인 페이지 로드 확인
3. `browser_take_screenshot` 호출 — `filename` 을 `.claude/logs/e2e/20260424-스크린샷-캡처-검증/login-page.png` 로 지정
4. `filename` 인자가 받아들여지지 않거나 경로 제약이 있으면 MCP 가 실제 저장한 경로를 로그 응답에서 확인해 대체 경로 문서화
5. `ls -la` + `file <path>` + `stat` 로 파일 존재·크기·PNG 시그니처 확인
6. 결과를 `.claude/logs/e2e/20260424-스크린샷-캡처-검증.md` 로 정리. 저장된 PNG 를 md 에서 참조하는 상대경로까지 검증
7. 백엔드/프론트엔드 프로세스 종료, 포트 응답 000 확인

## 검증 기준

- `login-page.png` 가 `.claude/logs/e2e/20260424-스크린샷-캡처-검증/` 아래에 생성됨
- 파일 크기 > 0 이고 `file` 명령으로 `PNG image data` 판정
- 로그 md 안의 `![](./20260424-스크린샷-캡처-검증/login-page.png)` 같은 상대참조가 실제 경로와 일치

## 비범위

- Flow 1~3 전체 재실행 (직전 턴에서 통과 확인됨)
- 증거 캡처 규칙의 네트워크·콘솔·snapshot 도구 검증 (이번엔 스크린샷만)
- `.gitignore` 정책 결정 (다음 이슈에서 실제 캡처 시점에)
- 비밀번호 재설정 (스크린샷 시점은 로그인 전 화면이라 계정 불필요)

## 주요 파일

- `.claude/knowledge/e2e-flows.md` — "증거 캡처" 섹션 컨벤션의 source
- `/tmp/start-backend.sh` — 백엔드 기동 래퍼 (기존 재사용)
- `.mcp.json` — Playwright MCP 설정 (변경 없음, 그대로 사용)

## 리스크

- MCP 서버 스펙상 `filename` 을 절대경로로 받는지 상대경로로 받는지 불명. 상대경로면 프로젝트 루트 기준 여부를 응답으로 확인 필요
- `browser_file_upload` 가 받아들이는 allowed roots(`.playwright-mcp/`, 프로젝트 루트)가 `take_screenshot` 에도 동일하게 적용되는지 불확실 → 제약 걸리면 `.playwright-mcp/` 내부로 저장 후 `logs/e2e/` 로 이동하는 대체 플로우 필요

---

## 회고 (Claude 자동 생성 — 사용자 채점 대기)

### 구현 결과
- 프론트엔드만 WSL 에서 기동 (백엔드 생략 — 로그인 화면 1컷이 목적이므로 계획 단계에서 범위 축소 반영)
- `browser_take_screenshot` 을 절대경로 `filename` 으로 호출 → `.claude/logs/e2e/20260424-스크린샷-캡처-검증/login-page.png` 저장 확인
- PNG 시그니처(`PNG image data, 1280x720`) + 크기(24,887 B) 검증 통과
- 결과 로그 `.claude/logs/e2e/20260424-스크린샷-캡처-검증.md` 작성, 상대경로로 스크린샷 참조

### 변경 파일
- `.claude/logs/e2e/20260424-스크린샷-캡처-검증.md` (신규)
- `.claude/logs/e2e/20260424-스크린샷-캡처-검증/login-page.png` (신규)

### E2E 검증 (Playwright MCP)
- Flow 1 (로그인): ⏭ 해당 없음 — 이 플랜 자체가 스크린샷 동작 검증이라 Flow 전체 재실행 불필요
- Flow 2 (업로드): ⏭ 해당 없음
- Flow 3 (공유): ⏭ 해당 없음
- **스냅샷**: `.claude/logs/e2e/20260424-스크린샷-캡처-검증/login-page.png`

### 이슈·학습
- 플랜에 적어둔 리스크 2개(절대/상대경로 여부, allowed roots 제약) 모두 **해소**됨. 절대경로 OK, 제약은 `file_upload` 만 타이트하고 screenshot 은 프로젝트 루트 하위면 어디든 가능
- MCP 응답이 절대경로 입력을 상대경로로 축약해 표시 → 실제 저장 위치는 `ls`/`stat` 으로 따로 확인해야 함
- 한글 디렉토리명(`20260424-스크린샷-캡처-검증`) 도 그대로 사용 가능

### 아쉬움·개선점
- 리스크 섹션을 실행 결과에 맞춰 플랜 파일 본문에서도 "해소됨" 으로 표기하지 않고 결과 로그에만 남긴 상태. 플랜 파일은 이번 회고로만 업데이트됨
- 플랜이 프로젝트 `.claude/plans/` 가 아닌 `~/.claude/plans/` 에 작성돼 `track-active-plan.sh` 훅의 프론트매터 자동 주입 흐름을 일부 우회함 (아카이빙 스크립트가 보완)
- `type: 'png'` 은 스키마상 default 라 생략 가능한데 명시적으로 넘겼음 — 다음엔 생략해도 무방

---
**채점 대기**: 위 회고를 읽은 뒤 프론트매터의 `score: null` 을 1~5 정수로 수정하세요.
필요하면 아래 "사용자 채점 메모" 섹션에 본인 의견을 덧붙이세요.

### 사용자 채점 메모
_자유 작성_
