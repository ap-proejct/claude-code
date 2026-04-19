# Google Drive 클론 프로젝트 계획서

## 기술 스택

| 영역 | 기술 |
|------|------|
| 언어 | Kotlin 2.2 |
| 프레임워크 | Spring Boot 4.0.5 |
| 웹 | Spring MVC + Thymeleaf |
| 보안 | Spring Security 6 |
| ORM | Spring Data JPA (Hibernate) |
| DB | H2 (개발) → 추후 PostgreSQL 전환 가능 |
| 빌드 | Gradle Kotlin DSL |
| 테스트 | JUnit 5 + Spring Boot Test |

---

## 핵심 기능 목록

### 1. 인증 / 사용자 관리
- [ ] 회원가입 (이메일 + 비밀번호)
- [ ] 로그인 / 로그아웃
- [ ] Remember-me 쿠키
- [ ] 비밀번호 변경
- [ ] 사용자 프로필 (이름, 이메일, 스토리지 사용량)

### 2. 파일 시스템 (핵심)
- [ ] 파일 업로드 (단일 / 다중)
- [ ] 파일 다운로드
- [ ] 파일 삭제 (휴지통으로 이동)
- [ ] 파일 이름 변경
- [ ] 파일 이동 (폴더 간)
- [ ] 파일 미리보기 (이미지, 텍스트, PDF)
- [ ] 파일 상세 정보 (크기, 날짜, 타입, 소유자)

### 3. 폴더 관리
- [ ] 폴더 생성
- [ ] 폴더 이름 변경
- [ ] 폴더 삭제 (내부 파일 포함)
- [ ] 폴더 이동
- [ ] 중첩 폴더 (트리 구조)
- [ ] 폴더 경로 표시 (breadcrumb)

### 4. 그룹 관리
- [ ] 그룹 생성 / 해산
- [ ] 그룹 멤버 초대 / 추방
- [ ] 그룹 내 역할 부여 (OWNER / MANAGER / MEMBER)
- [ ] 그룹 목록 / 상세 조회
- [ ] 그룹 탈퇴

### 5. 권한(Permission) 관리
- [ ] 파일/폴더에 **특정 사용자** 권한 부여
- [ ] 파일/폴더에 **그룹** 권한 부여
- [ ] 공개 링크 공유 (링크를 아는 누구나)
- [ ] 권한 등급: `OWNER` / `EDITOR` / `VIEWER`
- [ ] 권한 수정 / 회수
- [ ] 폴더 권한의 하위 파일 상속
- [ ] 공유 링크 만료 설정

### 6. 즐겨찾기 / 별표
- [ ] 파일/폴더 별표 추가
- [ ] 별표 목록 페이지

### 7. 휴지통
- [ ] 삭제된 파일 보관
- [ ] 휴지통 복구
- [ ] 영구 삭제
- [ ] 30일 후 자동 영구 삭제

### 8. 검색
- [ ] 파일명 검색
- [ ] 파일 타입 필터
- [ ] 날짜 범위 필터
- [ ] 소유자 필터

### 9. 정렬 / 뷰 모드
- [ ] 그리드 뷰 / 리스트 뷰 전환
- [ ] 이름 / 날짜 / 크기 정렬

### 10. 스토리지 관리
- [ ] 사용자별 스토리지 용량 제한 (기본 15GB)
- [ ] 스토리지 사용량 표시

---

## 데이터베이스 설계

### ERD 개요

```
users ──< files (self-join: parent_id)
users ──< group_members >── groups
files ──< file_permissions ──> users      (사용자 직접 권한)
                            ──> groups    (그룹 권한)
                            (share_token) (링크 공유)
```

### 권한 등급 정의

| 등급 | 설명 |
|------|------|
| `OWNER` | 파일 소유자. 삭제·권한부여·소유권이전 가능. 파일당 1명 |
| `EDITOR` | 업로드·수정·이름변경·이동 가능. 삭제·권한변경 불가 |
| `VIEWER` | 조회·다운로드만 가능 |

> 권한 우선순위: **OWNER > EDITOR > VIEWER**
> 같은 파일에 사용자 직접 권한과 그룹 권한이 겹치면 **더 높은 권한** 적용.

### 테이블 설계

#### `users` (사용자)
```sql
CREATE TABLE users (
    id                   BIGINT PRIMARY KEY AUTO_INCREMENT,
    email                VARCHAR(255) NOT NULL UNIQUE,
    password             VARCHAR(255) NOT NULL,          -- BCrypt 해시
    name                 VARCHAR(100) NOT NULL,
    system_role          VARCHAR(20) NOT NULL DEFAULT 'USER',  -- USER, ADMIN
    storage_limit_bytes  BIGINT NOT NULL DEFAULT 16106127360,  -- 15GB
    storage_used_bytes   BIGINT NOT NULL DEFAULT 0,
    created_at           TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at           TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
```

#### `groups` (그룹)
```sql
CREATE TABLE groups (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    name        VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    created_by  BIGINT NOT NULL REFERENCES users(id),
    created_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
```

#### `group_members` (그룹 멤버십)
```sql
CREATE TABLE group_members (
    id         BIGINT PRIMARY KEY AUTO_INCREMENT,
    group_id   BIGINT NOT NULL REFERENCES groups(id) ON DELETE CASCADE,
    user_id    BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    group_role VARCHAR(20) NOT NULL DEFAULT 'MEMBER',  -- OWNER, MANAGER, MEMBER
    joined_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uq_group_member UNIQUE (group_id, user_id)
);
```

> - `OWNER`: 그룹 해산, 멤버 역할 변경 가능 (그룹 생성자가 자동 부여)
> - `MANAGER`: 멤버 초대/추방 가능
> - `MEMBER`: 그룹에 공유된 파일 접근만 가능

#### `files` (파일 + 폴더 통합)
```sql
CREATE TABLE files (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    owner_id        BIGINT NOT NULL REFERENCES users(id),
    parent_id       BIGINT REFERENCES files(id),   -- NULL이면 루트
    name            VARCHAR(255) NOT NULL,
    type            VARCHAR(10) NOT NULL,           -- FILE | FOLDER
    mime_type       VARCHAR(100),                   -- 폴더는 NULL
    size_bytes      BIGINT NOT NULL DEFAULT 0,
    storage_path    VARCHAR(500),                   -- UUID 기반 실제 경로 (폴더는 NULL)
    is_starred      BOOLEAN NOT NULL DEFAULT FALSE,
    is_trashed      BOOLEAN NOT NULL DEFAULT FALSE,
    trashed_at      TIMESTAMP,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uq_file_name UNIQUE (parent_id, name, is_trashed)
);
```

> **설계 의도**: 파일과 폴더를 하나의 테이블로 관리 (Adjacency List 패턴).
> `owner_id`는 "소유자"이며 항상 암묵적으로 OWNER 권한을 가짐 (별도 `file_permissions` 행 불필요).

#### `file_permissions` (파일/폴더 권한 — 핵심)
```sql
CREATE TABLE file_permissions (
    id               BIGINT PRIMARY KEY AUTO_INCREMENT,
    file_id          BIGINT NOT NULL REFERENCES files(id) ON DELETE CASCADE,
    granted_by_id    BIGINT NOT NULL REFERENCES users(id),

    -- 대상: 셋 중 정확히 하나만 NOT NULL
    user_id          BIGINT REFERENCES users(id) ON DELETE CASCADE,
    group_id         BIGINT REFERENCES groups(id) ON DELETE CASCADE,
    share_token      VARCHAR(64) UNIQUE,           -- NULL이 아니면 링크 공유

    permission       VARCHAR(20) NOT NULL,         -- EDITOR | VIEWER  (OWNER는 files.owner_id로만 관리)
    inherit_to_children BOOLEAN NOT NULL DEFAULT TRUE,  -- 폴더 권한 하위 전파 여부
    expires_at       TIMESTAMP,                    -- NULL이면 만료 없음
    created_at       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- 동일 파일에 같은 대상 중복 방지
    CONSTRAINT uq_perm_user  UNIQUE (file_id, user_id),
    CONSTRAINT uq_perm_group UNIQUE (file_id, group_id),

    -- 정확히 하나의 대상만 지정 강제
    CONSTRAINT chk_target CHECK (
        (user_id IS NOT NULL)::INT +
        (group_id IS NOT NULL)::INT +
        (share_token IS NOT NULL)::INT = 1
    )
);
```

#### `starred_files` (별표 — 사용자별 독립)
```sql
CREATE TABLE starred_files (
    user_id    BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    file_id    BIGINT NOT NULL REFERENCES files(id) ON DELETE CASCADE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id, file_id)
);
```

> **왜 `files`의 컬럼이 아닌 별도 테이블?**
> 공유받은 파일도 각자가 독립적으로 별표를 관리해야 하기 때문.

#### `user_sessions` (Spring Security 자동 생성)
Spring Security의 `remember-me` 토큰 테이블은 자동 생성됨.

---

### 권한 판단 로직

특정 사용자가 파일에 어떤 권한을 갖는지 계산하는 순서:

```
1. files.owner_id == 현재 사용자  →  OWNER (최고 권한, 즉시 반환)

2. file_permissions 에서 user_id == 현재 사용자인 행 조회  →  직접 부여 권한

3. 현재 사용자가 속한 group_id 들 조회 후,
   file_permissions 에서 group_id IN (내 그룹들) 인 행 조회  →  그룹 권한

4. 2번과 3번 중 더 높은 권한 선택
   (EDITOR > VIEWER)

5. 아무것도 없으면 → 권한 없음 (403)
```

폴더에 권한을 부여할 때 `inherit_to_children = TRUE`이면,
하위 파일/폴더 조회 시 **부모 폴더의 권한을 상속**으로 처리.
(DB에 하위 항목마다 행을 추가하는 대신, 서비스 레이어에서 부모를 순회하며 판단)

---

## 패키지 구조

```
src/main/kotlin/demo/drive/
├── DriveApplication.kt
│
├── config/
│   ├── SecurityConfig.kt          # Spring Security 설정
│   ├── WebMvcConfig.kt            # 정적 리소스, 파일 업로드 크기 등
│   └── StorageConfig.kt           # 파일 저장 경로 설정
│
├── domain/
│   ├── user/
│   │   ├── User.kt                # @Entity
│   │   ├── UserRepository.kt
│   │   ├── UserService.kt
│   │   └── UserController.kt      # /auth/**, /profile/**
│   │
│   ├── group/
│   │   ├── Group.kt               # @Entity
│   │   ├── GroupMember.kt         # @Entity (group_members)
│   │   ├── GroupRole.kt           # enum: OWNER, MANAGER, MEMBER
│   │   ├── GroupRepository.kt
│   │   ├── GroupMemberRepository.kt
│   │   ├── GroupService.kt
│   │   └── GroupController.kt     # /groups/**
│   │
│   ├── file/
│   │   ├── File.kt                # @Entity (파일+폴더)
│   │   ├── FileType.kt            # enum: FILE, FOLDER
│   │   ├── FileRepository.kt
│   │   ├── FileService.kt
│   │   └── FileController.kt      # /drive/**, /api/files/**
│   │
│   ├── permission/
│   │   ├── FilePermission.kt      # @Entity (file_permissions)
│   │   ├── Permission.kt          # enum: OWNER, EDITOR, VIEWER
│   │   ├── FilePermissionRepository.kt
│   │   ├── PermissionService.kt   # 권한 판단 로직 핵심
│   │   └── PermissionController.kt # /api/permissions/**
│   │
│   ├── share/
│   │   ├── ShareService.kt        # 링크 토큰 생성/검증
│   │   └── ShareController.kt     # /share/{token}
│   │
│   └── trash/
│       ├── TrashService.kt
│       └── TrashController.kt     # /trash/**
│
├── storage/
│   ├── StorageService.kt          # 인터페이스
│   └── LocalStorageService.kt     # 로컬 디스크 저장 구현
│
└── common/
    ├── exception/
    │   ├── DriveException.kt
    │   └── GlobalExceptionHandler.kt
    └── extension/
        └── SecurityExtensions.kt  # currentUser() 확장함수
```

---

## URL 구조

| URL | 메서드 | 설명 |
|-----|--------|------|
| `/` | GET | 루트 → `/drive`로 리다이렉트 |
| `/auth/login` | GET/POST | 로그인 |
| `/auth/register` | GET/POST | 회원가입 |
| `/drive` | GET | 내 드라이브 루트 |
| `/drive/folder/{id}` | GET | 폴더 내용 |
| `/drive/file/{id}/download` | GET | 파일 다운로드 |
| `/drive/file/{id}/preview` | GET | 파일 미리보기 |
| `/drive/starred` | GET | 별표 목록 |
| `/drive/shared-with-me` | GET | 나에게 공유된 파일 목록 |
| `/trash` | GET | 휴지통 |
| `/share/{token}` | GET | 공유 링크 접근 (인증 불필요) |
| `/groups` | GET/POST | 그룹 목록 / 그룹 생성 |
| `/groups/{id}` | GET | 그룹 상세 / 멤버 목록 |
| `/api/files/upload` | POST | 파일 업로드 (multipart) |
| `/api/files/{id}` | PATCH | 이름변경/이동 |
| `/api/files/{id}` | DELETE | 휴지통 이동 |
| `/api/folders` | POST | 폴더 생성 |
| `/api/files/{id}/star` | POST/DELETE | 별표 추가/제거 |
| `/api/permissions/file/{id}` | GET | 파일 권한 목록 조회 |
| `/api/permissions/file/{id}` | POST | 권한 추가 (사용자/그룹/링크) |
| `/api/permissions/{permId}` | PATCH | 권한 수정 |
| `/api/permissions/{permId}` | DELETE | 권한 회수 |
| `/api/groups/{id}/members` | POST | 멤버 초대 |
| `/api/groups/{id}/members/{uid}` | PATCH | 멤버 역할 변경 |
| `/api/groups/{id}/members/{uid}` | DELETE | 멤버 추방 |

---

## 구현 순서 (단계별)

### Phase 1 — 기반 구축 (1~2일)
1. **프로젝트 설정**
   - `application.yaml` 설정 (H2, JPA, 파일 업로드 크기)
   - 파일 저장 디렉토리 설정

2. **사용자 인증**
   - `User` 엔티티 + `UserRepository`
   - `SecurityConfig` (BCrypt, 로그인 URL, 권한 설정)
   - 회원가입 / 로그인 / 로그아웃 폼 + 컨트롤러

### Phase 2 — 파일 시스템 (3~4일)
3. **파일/폴더 엔티티**
   - `File` 엔티티 (Adjacency List)
   - `FileRepository` (parent로 조회, owner로 조회)

4. **스토리지 서비스**
   - `LocalStorageService`: UUID로 파일명 생성, 디스크 저장/삭제

5. **파일 업로드 / 다운로드**
   - `MultipartFile` 처리
   - `Content-Disposition` 헤더로 다운로드

6. **폴더 CRUD**
   - 생성, 이름변경, 이동, 삭제 (재귀)

7. **드라이브 UI (Thymeleaf)**
   - 파일/폴더 목록 (그리드/리스트)
   - Breadcrumb 네비게이션
   - 업로드 폼 (drag & drop)

### Phase 3 — 권한 / 그룹 (2~3일)
8. **그룹 관리**
   - `Group`, `GroupMember` 엔티티
   - 그룹 CRUD + 멤버 초대/추방/역할변경

9. **권한 시스템**
   - `FilePermission` 엔티티 (user_id | group_id | share_token)
   - `PermissionService.resolvePermission(fileId, userId)` — 권한 판단 로직
   - 모든 파일 접근 API에 권한 체크 적용
   - 폴더 권한 상속 (`inherit_to_children`)

10. **공유 링크**
    - 토큰 생성 (`SecureRandom` 64자 hex)
    - `/share/{token}` 공개 접근 (로그인 불필요)

### Phase 4 — 부가 기능 (2~3일)
11. **별표 / 휴지통**
    - `starred_files` 테이블로 사용자별 독립 별표
    - 휴지통 이동 / 복구 / 영구삭제

12. **검색**
    - JPA `LIKE` 쿼리로 파일명 검색
    - 권한 있는 파일만 결과에 포함

### Phase 5 — 마무리 (1~2일)
13. **스토리지 용량 계산**
    - 업로드/삭제 시 `storage_used_bytes` 갱신

14. **UI 개선**
    - 반응형 레이아웃 (Bootstrap 또는 Tailwind CDN)
    - 파일 타입 아이콘
    - 진행률 표시
    - 권한 관리 모달 (사용자/그룹 검색 + 권한 부여 UI)

---

## 주요 구현 포인트

### 파일 저장 경로 전략
```
storage/
└── {userId}/
    └── {uuid}.{ext}    ← DB의 storage_path에 저장
```
실제 파일명은 UUID로 저장하고, 원본 이름은 DB의 `name` 컬럼에 보관.

### 폴더 삭제 (재귀)
```kotlin
fun deleteRecursively(fileId: Long, userId: Long) {
    val file = fileRepository.findByIdAndOwnerId(fileId, userId)
    if (file.type == FileType.FOLDER) {
        fileRepository.findByParentId(fileId).forEach {
            deleteRecursively(it.id, userId)
        }
    } else {
        storageService.delete(file.storagePath)
    }
    fileRepository.delete(file)
}
```

### Spring Security 현재 사용자 가져오기
```kotlin
fun currentUser(): User =
    (SecurityContextHolder.getContext().authentication.principal as UserDetails)
        .let { userRepository.findByEmail(it.username)!! }
```

### 권한 판단 서비스 핵심 로직
```kotlin
@Service
class PermissionService(
    private val fileRepository: FileRepository,
    private val filePermissionRepository: FilePermissionRepository,
    private val groupMemberRepository: GroupMemberRepository,
) {
    /**
     * 파일에 대해 userId가 갖는 최종 권한 반환.
     * 없으면 null (= 403 처리)
     */
    fun resolvePermission(fileId: Long, userId: Long): Permission? {
        val file = fileRepository.findById(fileId).orElse(null) ?: return null

        // 1. 소유자 → OWNER
        if (file.ownerId == userId) return Permission.OWNER

        // 2. 직접 부여 권한
        val direct = filePermissionRepository
            .findByFileIdAndUserId(fileId, userId)?.permission

        // 3. 그룹 권한 (사용자가 속한 그룹 중 가장 높은 것)
        val myGroupIds = groupMemberRepository.findGroupIdsByUserId(userId)
        val viaGroup = filePermissionRepository
            .findByFileIdAndGroupIdIn(fileId, myGroupIds)
            .maxOfOrNull { it.permission }

        // 4. 둘 중 높은 것 선택
        val effective = listOfNotNull(direct, viaGroup).maxOrNull()

        // 5. 없으면 부모 폴더에서 상속 여부 확인
        if (effective == null && file.parentId != null) {
            val inherited = filePermissionRepository
                .findByFileIdAndInheritTrue(file.parentId!!, userId, myGroupIds)
            return inherited
        }

        return effective
    }

    fun requirePermission(fileId: Long, userId: Long, required: Permission) {
        val actual = resolvePermission(fileId, userId)
            ?: throw AccessDeniedException("접근 권한이 없습니다.")
        if (actual < required)
            throw AccessDeniedException("${required.name} 권한이 필요합니다.")
    }
}
```

### 공유 링크 보안
- 공개 공유 URL (`/share/{token}`)은 `SecurityConfig`에서 `permitAll()` 처리
- 토큰은 `SecureRandom`으로 생성한 64자 hex 문자열 사용
- `file_permissions.share_token`으로 조회 후 권한(VIEWER/EDITOR) 적용

---

## application.yaml 최종 설정

```yaml
spring:
  application:
    name: drive

  datasource:
    url: jdbc:h2:file:./data/drivedb;AUTO_SERVER=TRUE
    driver-class-name: org.h2.Driver

  jpa:
    hibernate:
      ddl-auto: update
    show-sql: false
    properties:
      hibernate:
        format_sql: true

  servlet:
    multipart:
      max-file-size: 512MB
      max-request-size: 512MB

  h2:
    console:
      enabled: true
      path: /h2-console

drive:
  storage:
    base-path: ./storage
  user:
    default-storage-limit-gb: 15
```

---

## 이후 확장 가능한 기능 (선택)

| 기능 | 설명 |
|------|------|
| 실시간 협업 | WebSocket으로 폴더 변경 알림 |
| 파일 버전 관리 | 수정 이력 보관 |
| 압축 다운로드 | 폴더를 ZIP으로 다운로드 |
| 이미지 썸네일 | 업로드 시 썸네일 자동 생성 |
| 활동 로그 | 파일 접근/수정 이력 |
| 관리자 페이지 | 전체 사용자/파일 관리 |
| 외부 스토리지 | S3 / MinIO로 `StorageService` 교체 |
