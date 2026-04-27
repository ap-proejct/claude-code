---
task_type: "test"
title: "캡처 도구 나머지 3종 검증 (snapshot·console·network)"
created: "2026-04-24"
archived: "2026-04-24"
score: null
harness_sha: "5810a1b"
absorbed: false
score_auto: 4
eval_notes: "회고가 균형 있게 솔직(실패 케이스 명시, 비번 plaintext 방치 인정), 회고 파일 목록과 실제 디스크 파일 100% 일치(0B 파일까지 정확). E2E는 도구 검증 목적상 Flow 1 실패 케이스만 수집해 만점은 어려움."
eval_at: "2026-04-27"
---
# 캡처 도구 나머지 3종 검증

## Context

2026-04-24 에 `browser_take_screenshot` 동작을 검증했고(플랜 `20260424-playwright-mcp-스크린샷-캡처-동작-검증`), `e2e-flows.md` "증거 캡처" 섹션에는 네 개 도구를 명시해뒀다. 나머지 세 개 — `browser_snapshot(filename)`, `browser_console_messages()`, `browser_network_requests()` — 가 실제로 파일 저장/포맷/경로 제약 면에서 어떻게 동작하는지 확인.

## 작업 단계

1. 프론트엔드만 WSL 기동 (로그인 페이지 도달이면 충분)
2. `http://localhost:5173/login` 이동
3. 세 도구 각각 호출 + 결과 파일 저장 방식 확인
   - `browser_snapshot(filename: '.claude/logs/e2e/20260424-캡처도구-나머지-검증/login-snapshot.md')` — 접근성 트리 md 로 저장되는지, 절대/상대 경로 처리 방식
   - `browser_console_messages()` — 응답으로 돌려주는지(파일 저장 옵션이 없다면 우리가 txt 로 써야 함)
   - `browser_network_requests()` — 응답 구조 확인 후 json 으로 저장
4. 결과 로그 `.claude/logs/e2e/20260424-캡처도구-나머지-검증.md` 작성 (3개 도구별 "어떻게 호출하면 어디 저장되는가" 규칙 확정)
5. 프론트엔드 종료

## 검증 기준

- snapshot md 생성 확인 + 파일 내용이 유효한 yaml/markdown
- console/network 응답 구조를 결과 md 에 샘플로 인용
- 저장 경로 컨벤션이 스크린샷과 동일한지 or 별도 규칙 필요한지 명시

## 비범위

- Flow 2·3 실행 (이번도 Flow 1 범위에서만)
- 전체 Flow 통합 회고 갱신 (이번엔 도구 동작 검증만, `e2e-flows.md` 컨벤션 수정은 검증 결과에 따라 후속)
- 비밀번호 재설정·백엔드 기동 (로그인 페이지 도달이 목표)

## 리스크

- `browser_console_messages`/`browser_network_requests` 는 응답으로만 데이터를 돌려주고 `filename` 인자가 없을 가능성. 그 경우 `Write` 툴로 직접 저장하는 2단계 패턴이 됨
- `browser_snapshot` 은 filename 인자가 있다고 이미 스키마에서 확인 (`select:` 로 로드할 때 본 스키마 기준)

---

## 회고 (Claude 자동 생성 — 사용자 채점 대기)

### 구현 결과
- 3개 도구(`browser_snapshot`, `browser_console_messages`, `browser_network_requests`) 모두 `filename` 인자 내장 확인 — 플랜 리스크에서 우려했던 "응답만 주고 파일 저장은 수동" 은 **해소**
- 각 도구 1회 호출해 로그인 페이지에서 증거 생성
- network 는 정적 페이지에서 0 바이트 문제 발견 → 실제 API 호출(로그인 시도) 트리거 후 재캡처로 의미있는 증거(`[POST] .../api/auth/login => [502] Bad Gateway`) 확보
- 결과 로그 `.claude/logs/e2e/20260424-캡처도구-나머지-검증.md` 작성, 규칙 업데이트 제안 3가지 포함(console level 권장, network requestBody 민감정보 경고, 확장자 컨벤션)

### 변경 파일
- `.claude/logs/e2e/20260424-캡처도구-나머지-검증.md` (신규)
- `.claude/logs/e2e/20260424-캡처도구-나머지-검증/login-snapshot.md`
- `.claude/logs/e2e/20260424-캡처도구-나머지-검증/login-console.txt`
- `.claude/logs/e2e/20260424-캡처도구-나머지-검증/login-network.json` (0 B 예시)
- `.claude/logs/e2e/20260424-캡처도구-나머지-검증/login-attempt-network.json` (비번 plaintext 포함 샘플)

### E2E 검증 (Playwright MCP)
- Flow 1 (로그인): ❌ 부분 진행 — 의도적으로 백엔드 끄고 **실패 케이스만 트리거** (네트워크 도구 샘플 수집용). 통과 검증 아님
- Flow 2 (업로드): ⏭ 해당 없음
- Flow 3 (공유): ⏭ 해당 없음
- **스냅샷**: `.claude/logs/e2e/20260424-캡처도구-나머지-검증/login-snapshot.md`

### 이슈·학습
- **network 파일 확장자**: `.json` 을 줘도 실제 저장 포맷은 텍스트("`[POST] url => [status]`" 라인). MCP 가 확장자 무시. 로그 파일명 컨벤션 재검토 필요
- **민감정보 노출**: `requestBody:true` 가 비밀번호 plaintext 그대로 기록. 증거 수집 편의와 보안 트레이드오프. 기본 `false` 유지하고 필요 시에만 켤 것
- **`static:false` 기본값**: 로그인처럼 정적 페이지는 출력 0 바이트. 증거가 필요한 시점(API 호출 전/후) 를 정확히 잡아야 함

### 아쉬움·개선점
- 규칙 업데이트 제안을 `e2e-flows.md` 에 실제 반영하지 않고 로그에만 기록 — 다음 플랜에서 반영하거나, 아예 즉시 반영하는 편이 정합성에 나았을 수 있음
- 비번 plaintext 포함 network 파일을 삭제 없이 방치. 테스트 환경이라 실제 위험은 없지만 컨벤션 상 스크러빙 루틴이 없음

---
**채점 대기**: 위 회고를 읽은 뒤 프론트매터의 `score: null` 을 1~5 정수로 수정하세요.
필요하면 아래 "사용자 채점 메모" 섹션에 본인 의견을 덧붙이세요.

### 사용자 채점 메모
_자유 작성_
