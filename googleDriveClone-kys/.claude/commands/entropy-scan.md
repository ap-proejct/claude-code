---
description: 하네스 엔트로피 스캔 — 미흡수 아카이브, 채점 누락, 미참조 파일, 미사용 훅 리포트 생성 (자동 삭제 없음)
allowed-tools: Bash, Read
---

`.claude/scripts/entropy-scan.sh` 실행 → `.claude/logs/entropy/YYYYMMDD.md` 리포트 생성.

## 스캔 항목

| ID | 항목 | 임계 |
|----|------|------|
| P0-1 | 미흡수 아카이브 (`absorbed:false`) | 30일 이상 |
| P0-2 | 채점 누락 (`score` & `score_auto` 모두 null) | 14일 이상 |
| P0-3 | knowledge/ 미참조 파일 | 다른 md 에서 0회 언급 |
| P0-4 | 미사용 훅 | activity 로그 30일간 호출 흔적 없음 |
| P1-5 | activity 신호잡음비 | 빈 cmd/file 비율 |
| P1-6 | 안티패턴 draft 누적 | draft > 3 시 경고 |

## 절차

1. `bash .claude/scripts/entropy-scan.sh` 실행
2. 출력의 리포트 경로 사용자에게 알림
3. **자동 삭제·PR 생성 절대 금지** — 사용자가 리포트 보고 수동 결정

## 사용 시점

- 주 1회 정기 점검 권장 (`/loop 1w /entropy-scan` 으로 스케줄링 가능)
- 새 모델/하네스 변경 후 (원본 설계 7번 항목)
- 누적 아카이브가 20건 넘었을 때

## 출력 예시

```
✅ 엔트로피 스캔 완료: .claude/logs/entropy/20260426.md
   미흡수 8 / 미채점 5 / 미참조 1 / 미사용훅 0
```

리포트 파일 끝에 "요약 액션 아이템" 체크리스트 자동 생성됨.
