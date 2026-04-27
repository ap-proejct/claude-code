---
description: 기존 아카이브 플랜을 harness-evaluator 로 (재)채점해 score_auto 갱신
allowed-tools: Bash, Read, Edit, Agent, Glob
---

기존 아카이브 플랜에 대해 harness-evaluator 서브에이전트를 호출해 `score_auto` / `eval_notes` / `eval_at` 을 채우거나 갱신합니다.

## 사용 시나리오

- 과거 미채점(`score_auto: null`) 플랜 일괄 채점
- 회고 수정 후 재평가 필요
- evaluator 프롬프트/기준 변경 후 전체 재채점

## 입력

`$ARGUMENTS` — 다음 중 하나
- 아카이브 파일명 (예: `20260424-드래그-이동.md`) — 단일 채점
- `null` — `score_auto: null` 인 모든 아카이브 일괄 채점
- 비어있음 — 사용자에게 어떤 모드인지 묻기

## 절차

1. **대상 파일 결정**
   - $ARGUMENTS 가 파일명: `.claude/plans/archived/<filename>` 단일
   - $ARGUMENTS == "null": Glob 으로 `.claude/plans/archived/*.md` 전체 → 각 파일 Read 해서 `score_auto: null` 인 것만 추림
   - $ARGUMENTS 비어있음: AskUserQuestion 으로 단일/전체 선택 받기

2. **각 대상에 대해 반복**
   - Agent tool, `subagent_type: harness-evaluator`, 프롬프트: "다음 아카이브 플랜을 평가하세요: `<absolute_path>`"
   - 반환된 JSON 파싱
   - Edit 으로 프론트매터 갱신:
     ```yaml
     score_auto: <score>
     eval_notes: "<notes>"
     eval_at: "<오늘 ISO date>"
     ```
   - `score:` (사용자 채점) 는 절대 건드리지 않음

3. **결과 리포트**
   - 처리 건수, 점수 분포 (1점 N건, 2점 N건 ...) 출력
   - 평균 점수
   - suggestions 는 단일 모드에서만 표시 (전체 모드는 너무 길어짐)

## 실패 처리

- evaluator 가 JSON 파싱 실패: 1회 재시도 → 그래도 실패 시 해당 파일은 스킵하고 카운트
- 처리 후 실패한 파일 목록을 마지막에 표시
