---
task_type: "feat"
title: "Phase 3 /absorb + Phase C-1 Playwright MCP E2E 하네스"
created: "2026-04-23"
archived: "2026-04-23"
score: null
harness_sha: "5810a1b"
absorbed: false
score_auto: 4
eval_notes: "하네스 설정 변경 작업이라 E2E 생략은 정당하나 회고에 적힌 'e2e-flows.md 58줄'이 실제 98줄로 불일치. 회고 균형(아쉬움·리스크 이월·소급 플랜 절차 비용)은 매우 정직."
eval_at: "2026-04-27"
---
# Phase 3 /absorb + Phase C-1 Playwright MCP E2E 하네스

## Context

하네스 엔지니어링 2개 챕터를 같은 세션에서 처리했기 때문에 소급 플랜으로 묶어서 아카이빙한다 (B안).

- **Phase 3**: 안티패턴 확정 루프의 마지막 조각. 지금까진 `/antipattern-scan` 으로 초안이 생겨도 소스 플랜의 `absorbed` 상태를 바꾸는 수단이 없어 트렌드 집계에서 계속 카운트됨.
- **Phase C-1**: "에이전트 워크플로우 하네스" 철학에 맞게, Claude 가 UI 변경 작업의 완성도를 스스로 검증하게 만든다. Playwright MCP 를 프로젝트 스코프로 붙이고 골든 패스를 knowledge 로 고정.

## 범위

### Phase 3
- `/absorb <안티패턴 md>` 커맨드 도입
- 안티패턴 `status: draft → confirmed` + `confirmed_at` 라인 주입
- 소스 플랜들에 `absorbed: true` 주입 (이미 true 면 skip, 라인 없으면 삽입)
- 마지막에 `plan-resync.sh` 명시 호출 (bash sed 편집은 Layer A 훅 대상 아님)

### Phase C-1
- `.mcp.json` 으로 Playwright MCP 프로젝트 스코프 등록 (user settings 안 건드림)
- `knowledge/e2e-flows.md` — 골든 패스 3개 (로그인 / 업로드 / 공유) + 검증 선택 기준
- `/task-archive` 절차에 "3-1 E2E 검증 판단" + 회고 템플릿에 "E2E 검증" 섹션

## 비범위

- `@playwright/test` CLI 기반 자동 회귀 (안 2, 유지보수 코스트 커서 보류)
- `pre-archive` 훅으로 E2E 섹션 채움 강제 (후속 챕터 후보)
- 자동 채점 연동

## 검증

- `/absorb` 는 /tmp fixture 로 스모크 테스트 완료 (멱등성, 인자 누락, 경로 해석 3케이스)
- Playwright MCP 서버 실제 기동·연결은 Claude Code 재시작 필요 → 이 플랜 범위 밖

---

## 회고 (Claude 자동 생성 — 사용자 채점 대기)

### 구현 결과
- Phase 3: `/absorb` 커맨드 md + 스크립트 구현. 안티패턴 프론트매터에서 `source_plans` 파싱 → 각 플랜에 `absorbed: true` 주입, 자신은 `status: confirmed` + `confirmed_at` 기록, 마지막에 `plan-resync.sh` 호출
- Phase C-1: 프로젝트 로컬 `.mcp.json` 에 Playwright MCP 등록, `knowledge/e2e-flows.md` 58줄로 골든 패스 3개(로그인/업로드/공유) + 검증 선택 기준 + 회고 포맷 문서화, `/task-archive` 회고 템플릿에 "E2E 검증" 섹션 추가
- CLAUDE.md 워크플로우 5 → 6단계 확장, knowledge 디렉토리 설명에 e2e-flows.md 언급 추가

### 변경 파일
- `.claude/commands/absorb.md` (신규)
- `.claude/scripts/absorb.sh` (신규, +x)
- `.claude/commands/task-archive.md` (3-1 단계 + 회고 템플릿 E2E 섹션 추가)
- `.claude/knowledge/e2e-flows.md` (신규, 58줄)
- `.mcp.json` (신규, 프로젝트 스코프 MCP)
- `CLAUDE.md` (워크플로우 6단계 + knowledge 설명)

### E2E 검증 (Playwright MCP)
- Flow 1 (로그인): ⏭ 해당 없음
- Flow 2 (업로드): ⏭ 해당 없음
- Flow 3 (공유): ⏭ 해당 없음
- **스냅샷**: 미저장
- (이번 작업은 하네스 설정 변경·문서화만 있고 UI·Controller·Service 에 손대지 않아 knowledge 의 "검증 선택 기준" 상 E2E 생략 대상)

### 이슈·학습
- 소급 플랜 아카이빙 경로 확인: track-active-plan.sh 훅은 `.claude/plans/*.md` Write 만 감지하므로, 플랜을 미리 작성해두지 않고 바로 작업해버리면 `state/current-plan.md` 가 비어 `/task-archive` 가 거부됨. B안으로 소급 플랜을 지금 작성해 포인터 세팅 후 아카이빙하는 우회가 필요했음
- Playwright MCP 검증을 "Claude 스스로 한다" 신뢰 모델이라 UI 변경이 있어도 Claude 가 실행을 누락하면 회고에 공란이 남을 수 있음 — 강제성은 후속 `pre-archive` 훅에서 가능
- `.mcp.json` 프로젝트 스코프는 user `~/.claude.json` 건드리지 않아 환경 격리가 깔끔했지만, 실제 서버 기동은 다음 세션 재시작 이후라 이번 세션에선 MCP 도구 호출을 테스트 못 했음

### 아쉬움·개선점
- Phase 3 와 Phase C-1 을 분리된 플랜으로 선(先)작성하지 않고 대화 상에서 "스케일 작으니 Plan 모드 생략" 으로 직행함. 결과적으로 소급 플랜이 필요해 절차 비용 발생 — 다음엔 중간 규모도 플랜부터 쓰는 게 루프 마찰이 적을 듯
- Playwright MCP 를 붙여놓고도 실제 로그인 → 업로드 플로우 한 번도 못 굴려봄. 다음 UI 플랜에서 처음 돌 때 셀렉터 / 타이밍 이슈가 나올 가능성이 있어 리스크가 이월됨
- `e2e-flows.md` 의 선택 기준이 느슨(UI·Controller·Service 수정 시)해서 Claude 가 어느 범위까지 "Controller 수정" 으로 볼지 실전에서 엣지케이스 나올 것

---
**채점 대기**: 위 회고를 읽은 뒤 프론트매터의 `score: null` 을 1~5 정수로 수정하세요.
필요하면 아래 "사용자 채점 메모" 섹션에 본인 의견을 덧붙이세요.

### 사용자 채점 메모
_자유 작성_
