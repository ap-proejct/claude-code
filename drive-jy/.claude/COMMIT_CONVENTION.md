# Git 커밋 컨벤션 (Angular Style)

커밋 메시지는 **Angular Commit Message Convention** 을 따른다.  
참고: https://github.com/angular/angular/blob/main/CONTRIBUTING.md#commit

---

## 형식

```
<type>(<scope>): <subject>

<body>         ← 선택사항. 무엇을, 왜 변경했는지
<footer>       ← 선택사항. 이슈 참조, BREAKING CHANGE
```

- **헤더 한 줄 = 72자 이하**
- **subject**: 현재 시제, 소문자 시작, 끝에 `.` 금지
- **body**: 각 줄 100자 이하

---

## type 목록

| type | 언제 사용 |
|------|----------|
| `feat` | 새 기능 추가 |
| `fix` | 버그 수정 |
| `refactor` | 기능 변경 없는 코드 개선 |
| `test` | 테스트 추가/수정 |
| `docs` | 문서, 주석, CLAUDE.md 변경 |
| `style` | 포맷, 공백 등 코드 의미 변화 없음 |
| `chore` | 빌드 설정, 의존성, 설정 파일 |
| `perf` | 성능 개선 |

---

## scope 목록 (이 프로젝트 기준)

| scope | 대상 |
|-------|------|
| `user` | 사용자 도메인 (User, UserService, AuthController) |
| `file` | 파일/폴더 도메인 (File, FileService, FileController) |
| `group` | 그룹 도메인 (Group, GroupService) |
| `permission` | 권한 도메인 (FilePermission, PermissionService) |
| `share` | 공유 링크 (ShareService, ShareController) |
| `trash` | 휴지통 (TrashService, TrashController) |
| `auth` | 인증/인가 (SecurityConfig, UserPrincipal) |
| `storage` | 파일 저장소 (LocalStorageService) |
| `common` | 공통 (DriveException, CommonResponse, GlobalExceptionHandler) |
| `infra` | 인프라 설정 (application.yaml, schema.sql, JPA 설정) |
| `ui` | Thymeleaf 템플릿, CSS |
| `test` | 테스트 인프라 (IntegrationTestBase, FakeRepository) |
| `hooks` | Claude Code hook, 설정 파일 |

---

## 예제

### feat — 새 기능

```
feat(file): add file upload with size validation

- POST /api/files/upload 엔드포인트 구현
- 512MB 초과 시 STORAGE_LIMIT_EXCEEDED 예외 처리
- LocalStorageService.store() 로 UUID 기반 저장
```

```
feat(user): implement user registration with email duplicate check
```

```
feat(share): generate secure share token using SecureRandom
```

### fix — 버그 수정

```
fix(permission): resolve null pointer when user has no group membership
```

```
fix(auth): redirect to login page on session expiration instead of 500
```

```
fix(file): prevent path traversal attack by rejecting '..' in file path
```

### test — 테스트 추가

```
test(file): add FileServiceTest with FakeFileRepository

- 파일 업로드 성공/실패 케이스
- 스토리지 초과 예외 검증
- FakeFileRepository 인메모리 구현
```

```
test(user): add UserJpaRepositoryTest extending DataJpaTestBase
```

### refactor — 리팩토링

```
refactor(permission): extract permission resolution logic to separate method

MAX_INHERIT_DEPTH 상수 도입, resolvePermission 가독성 개선
```

### docs — 문서

```
docs(hooks): add mandatory workflow enforcement via Claude Code hooks
```

```
docs: update CLAUDE.md with plan-first workflow and commit convention
```

### chore — 설정

```
chore(infra): add application-test.yaml for test profile
```

```
chore: configure .gitignore for storage/ and claude state files
```

---

## BREAKING CHANGE

기존 API 시그니처가 변경될 경우 footer에 명시:

```
feat(permission): change permission level from String to enum

BREAKING CHANGE: PermissionService.requirePermission() 두 번째 인자가
String → Permission enum으로 변경됨.
호출부 전체를 Permission.EDITOR 형태로 수정 필요.
```

---

## 잘못된 예 vs 올바른 예

| ❌ 잘못된 예 | ✅ 올바른 예 |
|------------|------------|
| `fix bug` | `fix(auth): handle expired session gracefully` |
| `작업 완료` | `feat(file): add folder creation endpoint` |
| `WIP` | `test(group): add GroupServiceTest with FakeGroupRepository` |
| `Update User.kt` | `refactor(user): remove unused storageUsedBytes setter` |
| `커밋` | `chore(infra): update application-dev.yaml H2 datasource url` |
