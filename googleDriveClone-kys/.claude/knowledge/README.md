# 지식 베이스 (Knowledge Base)

프로젝트의 설계 결정, 도메인 지식, 운영 규칙을 버전 관리되는 형태로 보관한다.
이 폴더는 하네스 원칙 5번("모든 지식은 에이전트가 참조할 수 있도록 리포지토리 안에 버전 관리되는 형태로 있어야 한다")의 실행 단위다.

## 작성 규칙

1. **파일당 최대 150줄** — 200줄에 여유를 두어 성장 버퍼 확보. 초과 시 하위 디렉토리로 분할한다.
2. **topic 1개 = 파일 1개** — 혼합 금지. 예: `auth.md` 는 인증만, `file-access.md` 는 파일 접근 권한만.
3. **파일명은 kebab-case** — `file-access.md`, `s3-integration.md` 형식.
4. **서두에 1~2줄 요약** — 에이전트가 grep으로 찾았을 때 첫 줄만 보고 해당 파일인지 판단 가능해야 함.
5. **상호 링크는 상대 경로로** — `[파일 접근 권한](../file-access.md)` 형식. 외부 링크는 최소화.

## 파일 유형

| 접두사 | 용도 | 예시 |
|--------|------|------|
| `adr-` | Architecture Decision Record | `adr-001-jwt-over-session.md` |
| (없음) | 도메인/기능 지식 | `file-access.md`, `share-permission.md` |
| `runbook-` | 운영 절차 | `runbook-mysql-restore.md` |

## 하위 디렉토리

규모가 커지면 domain별로 분할한다:

```
knowledge/
  README.md
  adr-001-jwt-over-session.md
  antipatterns/        # /antipattern-scan 이 생성하는 초안 보관
    20260423-검증.md    # pattern 프론트매터 + 사용자가 수동 요약
  auth/
    jwt-flow.md
  file/
    access-control.md
```

분할 시점은 **단일 파일이 150줄을 넘을 때**이며, 분할 후에도 상호 링크를 유지한다.

### `antipatterns/` 관리

- `/antipattern-scan` 슬래시 커맨드가 점수 ≤2 플랜 3건 이상 공통 키워드로 초안 자동 생성
- 초안의 `status: draft` 는 사용자가 검토 후 `status: confirmed` 로 변경 (수동)
- 확정된 안티패턴만 Phase 3 의 `/absorb` 대상이 되어 원본 플랜 `absorbed:true` 처리

## 금지 사항

- **일시적 작업 내용 기록 금지** — 진행 중 작업은 `.claude/plans/` 또는 `.claude/state/` 에 둔다.
- **코드 스니펫 전체 복사 금지** — 파일 경로와 함수명 참조로 충분. 코드는 repo 안에 이미 있다.
- **외부 URL 의존 금지** — 블로그 링크가 썩어도 문서가 살아야 한다. 필요한 내용은 요약해 담는다.

## 리뷰 주기

월 1회 엔트로피 GC 시(Phase 4에서 도입 예정) 다음을 확인한다:
- 6개월 이상 변경 없고 코드와 불일치하는 파일 → 제거 또는 갱신
- 참조 0회 파일 → 제거
- 같은 topic의 중복 파일 → 병합
