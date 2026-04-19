# Google Drive 클론 — 프로젝트 개요

## 응답 언어 (반드시 준수)

**모든 응답은 한국어로 작성한다.**

- 사용자와의 대화, 설명, 보고, 에러 메시지 모두 한국어
- 코드 주석도 한국어 우선 (영문 라이브러리 용어는 그대로 사용)
- 커밋 메시지의 subject 는 영문 허용 (Angular Convention), body 는 한국어
- 변수명/함수명/클래스명은 영문 (Kotlin 표준)

---

## 필수 워크플로우 (반드시 준수)

이 프로젝트의 모든 작업은 아래 순서를 **반드시** 따른다. **Hook이 단계 누락을 자동 차단한다.**

### 선행 조건 (최초 1회)

```bash
git init && git add . && git commit -m "초기 커밋"   # git 저장소 초기화
chmod +x .claude/hooks/*.sh                         # hook 실행 권한
```

---

### 0. 작업 시작 전 — 계획서 작성 (Hook 강제)

**`.claude/state/current-plan.md` 에 계획을 먼저 작성해야 한다.**  
미작성 시 `require-plan.sh` hook이 코드 수정을 차단한다.

```markdown
## 작업 목표
- 무엇을 왜 변경하는지 설명

## 변경할 파일
- src/main/kotlin/demo/drive/...

## 완료 조건
- compileKotlin 통과, 테스트 통과, git commit
```

추가로:
- 수정할 레이어의 CLAUDE.md를 먼저 읽는다
- 수정 대상 파일을 Read로 먼저 읽은 뒤 수정한다 — 추측 금지
- 현재 Phase 확인 — 이전 Phase 미완 시 해당 Phase부터 완료 후 진행

---

### 1. Kotlin 파일 작성/수정 직후 (Hook 자동 실행)

`compile-kotlin.sh` hook이 **자동으로** `./gradlew.bat compileKotlin` 을 실행한다.  
실패 시 stderr 로 즉시 피드백 — 수정 후 다음 단계로 진행.

---

### 2. 새 도메인/엔티티 추가 시 — 테스트 5종 세트 필수 생성

| 파일 | 위치 |
|------|------|
| `Fake{Domain}Repository.kt` | `src/test/kotlin/demo/drive/{domain}/fake/` |
| `{Domain}ServiceTest.kt` | `src/test/kotlin/demo/drive/{domain}/service/` |
| `{Domain}ControllerTest.kt` | `src/test/kotlin/demo/drive/{domain}/controller/` |
| `{Domain}JpaRepositoryTest.kt` | `src/test/kotlin/demo/drive/{domain}/infrastructure/` |
| `{domain}.spec.ts` (**E2E**) | `e2e/specs/{domain}/` |

- `harness` 에이전트로 자동 생성 가능 (5번째 E2E 스펙 포함).
- Controller 파일 생성 시 `require-e2e.sh` hook이 E2E 스펙 누락을 자동 감지.

---

### 3. 기능 구현 완료 후

```bash
./gradlew.bat test           # JUnit 테스트 (필수)
cd e2e && npx playwright test  # E2E 테스트 (서버 기동 상태에서)
```

JUnit 전체 테스트 통과 필수. **실패 테스트가 하나라도 있으면 작업 미완료.**  
E2E 테스트는 서버 기동(`./gradlew.bat bootRun`) 후 별도 실행한다.

#### ⚠️ 프론트엔드(HTML 템플릿) 수정 시 E2E 필수 (Hook 강제)

`src/main/resources/templates/**/*.html` 파일을 **하나라도** 수정하면:

1. JUnit 테스트 통과 후
2. **반드시 E2E 테스트를 실행**해야 한다 — 서버 기동 필수
3. E2E 통과 확인 후에만 커밋

**강제 메커니즘 (두 단계):**
- `git pre-commit hook` — E2E 리포트가 HTML 파일보다 오래됐으면 `git commit` 자체를 차단
- `require-commit.sh` Stop hook — 미커밋 HTML 파일이 있으면 응답 종료 차단

```bash
# 프론트엔드 변경 시 완료 순서 (반드시 이 순서)
./gradlew.bat test                      # 1. JUnit
./gradlew.bat bootRun                   # 2. 서버 기동 (별도 터미널)
cd e2e && npx playwright test           # 3. E2E (필수 — 생략 불가)
git add <html 파일들> && git commit ...  # 4. 커밋 (pre-commit hook 통과 후 완료)
```

---

### 4. 완료 보고 전 코드 리뷰 체크리스트

- `!!` 연산자 없음 → `?: throw DriveException(DriveErrorCode.XXX)` 패턴만 사용
- 모든 예외 → `DriveException(DriveErrorCode.XXX)` 단일 클래스
- 모든 REST 응답 → `CommonResponse<T>` 래핑
- Service 진입 시 권한 체크 (`permissionService.requirePermission()`)
- Mock 사용 없음 (외부 시스템 제외)
- BCrypt로 비밀번호 해싱, 평문 저장 절대 금지
- 파일 경로에 `..` 포함 시 즉시 거부 (Path Traversal 방어)

---

### 5. 작업 완료 후 — git commit (Hook 강제)

**커밋 없이 응답 종료 불가.** `require-commit.sh` hook이 미커밋 `.kt` 파일을 감지하면 차단한다.

**커밋 메시지는 Angular Convention 을 반드시 따른다** → @.claude/COMMIT_CONVENTION.md 참조.

형식:
```
<type>(<scope>): <subject>
```

예시:
```bash
git commit -m "feat(user): add registration endpoint with email validation"
git commit -m "fix(permission): resolve null check in resolvePermission"
git commit -m "test(file): add FakeFileRepository and FileServiceTest"
```

`type` 목록: feat / fix / refactor / test / docs / style / chore / perf  
`scope` 목록: user / file / group / permission / share / trash / auth / storage / common / infra / ui / test / hooks

커밋 성공 시 `post-commit-clear.sh` hook이 계획서를 `.claude/plans/archived/` 로 자동 이동한다.  
다음 작업은 새 계획서 작성부터 시작.

---

### 6. 위반 시 처리

사용자가 지적하지 않더라도 Claude 스스로 누락을 발견하면 즉시 해당 단계로 돌아가 보완한다.

---

### 7. 컨텍스트 자동 보존 (Hook 자동 동작)

Claude Code가 컨텍스트 한계에 도달해 자동 압축을 수행할 때:

1. **`pre-compact.sh`** 가 먼저 실행되어 다음을 `.claude/state/last-session.md` 에 저장:
   - 진행 중이던 계획서 (`current-plan.md`)
   - git 미커밋 변경 목록
   - 최근 커밋 5개
   - 10분 내 수정된 `.kt` 파일 목록

2. **다음 세션 시작 시 `session-start.sh`** 가 위 스냅샷을 자동으로 컨텍스트에 주입한다.

→ Claude는 압축 후에도 작업 흐름을 잃지 않고 이어갈 수 있다.  
→ 사용자가 따로 `/compact` 를 호출하거나 상태를 정리할 필요 없음.

---

## 프로젝트

Google Drive 클론 웹 애플리케이션. 파일/폴더 관리, 사용자·그룹 기반 권한 시스템, 공유 링크 제공.

## 기술 스택

| 영역 | 기술 |
|------|------|
| 언어 | Kotlin 2.2 |
| 프레임워크 | Spring Boot 4.0.5 |
| 웹 | Spring MVC + Thymeleaf |
| CSS | Tailwind CSS (CDN) |
| 보안 | Spring Security 6 (Jakarta EE) |
| ORM | Spring Data JPA (Hibernate) |
| DB | H2 (dev) / PostgreSQL (prod) |
| 빌드 | Gradle Kotlin DSL |
| 테스트 | JUnit 5 + Spring Boot Test |

## 레이어별 가이드

@src/main/kotlin/demo/drive/CLAUDE.md         ← 백엔드 (아키텍처 · 예외 · REST 응답 · 보안 규칙)
@src/main/resources/templates/CLAUDE.md       ← 프론트엔드 (Thymeleaf · Tailwind · 컴포넌트)
@src/test/kotlin/demo/drive/CLAUDE.md         ← 테스트 (계층 전략 · Fake 패턴 · 베이스 클래스)

## 빌드 & 실행

```bash
./gradlew compileKotlin          # 컴파일만 확인
./gradlew build                  # 전체 빌드 + 테스트
./gradlew bootRun                # 개발 서버 (http://localhost:8080)
```

Windows에서 `./gradlew` 실패 시 `./gradlew.bat` 사용.
