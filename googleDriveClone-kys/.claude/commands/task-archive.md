---
description: 활성 플랜을 회고와 함께 아카이빙 + harness-evaluator 자동 채점
allowed-tools: Bash, Read, Edit, Agent
---

활성 플랜을 아카이브로 전환합니다.

**채점 흐름 (생성/평가 분리):**
- `score`: 사용자 수동 채점 (1~5) — 사용자가 회고 검토 후 직접 수정
- `score_auto`: harness-evaluator 서브에이전트 자동 채점 (1~5) — cold-context 평가, 본 절차에서 자동 주입

## 절차

1. **활성 플랜 경로 확인**
   - `.claude/state/current-plan.md` 를 Read 로 읽어 경로 확보
   - 비어 있으면 중단하고 "활성 플랜이 없습니다. Plan 모드로 플랜을 먼저 작성하세요." 출력

2. **원 계획 파악**
   - 활성 플랜 파일 Read
   - 프론트매터의 `created` 날짜 기억

3. **실제 변경 수집** (현재 프로젝트 디렉토리로 스코프 제한 — 상위 repo의 다른 프로젝트 변경 제외)
   - `git log --since="<created>" --oneline -- .` 실행
   - `git diff --name-only -- .` 로 변경 파일 목록 확보 (대략적 기준으로 충분)
   - 대화 컨텍스트에서 작업 중 이슈·전환점 떠올리기

3-1. **E2E 검증 판단** — `.claude/knowledge/e2e-flows.md` 의 "검증 선택 기준" 을 따라
   - UI·Controller·Service 수정이 포함되면 관련 Flow 를 Playwright MCP 로 실제 실행 후 결과를 회고에 기록
   - 순수 백엔드·설정·`.claude/` 만 바뀌었으면 "해당 없음" 으로 기록하고 실행 생략
   - MCP 가 붙어 있지 않거나 서버가 기동 안 돼 있으면 "미실행(사유)" 로 기록

4. **회고 섹션 Edit 로 플랜 파일 맨 끝에 append**

   아래 구조를 그대로 넣되, `<...>` 부분만 실제 내용으로 채웁니다:

   ```markdown

   ---

   ## 회고 (Claude 자동 생성 — 사용자 채점 대기)

   ### 구현 결과
   - <원 계획 대비 실제로 구현된 것. 계획과 달라진 부분 포함>

   ### 변경 파일
   - <git diff --name-only 결과. 없으면 "없음">

   ### E2E 검증 (Playwright MCP)
   - Flow 1 (로그인): <✅ 통과 / ❌ 실패(사유) / ⏭ 해당 없음 / 미실행(사유)>
   - Flow 2 (업로드): <위와 동일>
   - Flow 3 (공유): <위와 동일>
   - **스냅샷**: <MCP 저장 경로 or "미저장">

   ### 이슈·학습
   - <작업 중 발견한 버그·설계 전환·예상 밖 이슈. 순탄했으면 "특이사항 없음">

   ### 아쉬움·개선점
   - <시간·범위 문제로 타협한 것, 자가 비판, 다음에 다르게 할 것>

   ---
   **채점 대기**: 위 회고를 읽은 뒤 프론트매터의 `score: null` 을 1~5 정수로 수정하세요.
   필요하면 아래 "사용자 채점 메모" 섹션에 본인 의견을 덧붙이세요.

   ### 사용자 채점 메모
   _자유 작성_
   ```

5. **아카이빙 스크립트 호출**
   - `bash .claude/scripts/task-archive.sh` 실행
   - 스크립트가 파일 이동·프론트매터 주입(score/score_auto/eval_notes/eval_at 모두 null)·state 초기화 담당
   - 출력에서 아카이브된 파일 경로 캡처

6. **harness-evaluator 서브에이전트 호출 (cold-context 자동 채점)**
   - Agent tool 로 `subagent_type: harness-evaluator` 호출
   - 프롬프트: "다음 아카이브 플랜을 평가하세요: `<archived_path>`"
   - 반환된 JSON 파싱:
     ```json
     {"score": 4, "axes": {...}, "notes": "...", "suggestions": [...]}
     ```
   - **suggestions 는 메인 세션에서 출력만, 파일에 쓰지 않음** (참고용)

7. **평가 결과 프론트매터 주입**
   - Edit 으로 archived 파일 프론트매터 갱신:
     ```yaml
     score_auto: <returned score>
     eval_notes: "<returned notes>"
     eval_at: "<오늘 ISO date>"
     ```
   - `score:` 는 그대로 null 유지 (사용자 수동 채점 슬롯)

8. **사용자에게 결과 요약**
   - 아카이브 경로, score_auto 점수, eval_notes 한 줄, suggestions 3개 출력
   - "사용자 score 채점 대기" 명시

## 회고 작성 원칙

- **자화자찬 금지** — "완벽히 구현했다" 같은 표현 피하기. evaluator 가 C축에서 감점함.
- **사실 기반** — 작업 중 실제로 있었던 전환·실패·재작업만 기술
- **짧게** — 각 섹션 1~4개 불릿. 길면 사용자가 안 읽음
- **특이사항 없으면 "없음"** — 억지로 채우지 말 것

## 실패 처리

- harness-evaluator 가 JSON 외 텍스트 반환 시: 한 번 재호출. 두 번째도 실패면 score_auto 는 null 유지하고 eval_notes 에 "evaluator 응답 파싱 실패" 기록
- Agent tool 사용 불가 환경: 6~7단계 스킵, 사용자에게 "evaluator 미실행" 명시
