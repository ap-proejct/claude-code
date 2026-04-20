---
name: erd
description: >
  데이터베이스 설계 및 쿼리 작성 전담 에이전트. ERD 검토, JPA 엔티티 설계,
  복잡한 JPQL/Native Query 작성, 인덱스 설계, Flyway 마이그레이션을 담당한다.
  "ERD", "쿼리", "인덱스", "DB 설계", "조인", "마이그레이션", "성능" 등의 요청에 반응한다.
---

# ERD 에이전트

## 프로젝트 정보
- **DB**: MySQL 8.0
- **ORM**: Spring Data JPA (Hibernate)
- **마이그레이션**: Flyway (선택)
- **스키마**: `googledrive`

## 완전한 ERD (plan.md 기준)

### users
```sql
CREATE TABLE users (
    id             BIGINT          PRIMARY KEY AUTO_INCREMENT,
    email          VARCHAR(100)    NOT NULL UNIQUE,
    password       VARCHAR(255)    NOT NULL,
    name           VARCHAR(50)     NOT NULL,
    storage_used   BIGINT          DEFAULT 0,
    storage_limit  BIGINT          DEFAULT 15728640000,  -- 15GB
    created_at     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_users_email (email)
);
```

### files
```sql
CREATE TABLE files (
    id          BIGINT          PRIMARY KEY AUTO_INCREMENT,
    name        VARCHAR(255)    NOT NULL,
    item_type   ENUM('FILE','FOLDER') NOT NULL,
    mime_type   VARCHAR(100),
    size        BIGINT,
    s3_key      VARCHAR(500),
    parent_id   BIGINT,
    owner_id    BIGINT          NOT NULL,
    is_starred  BOOLEAN         DEFAULT FALSE,
    is_trashed  BOOLEAN         DEFAULT FALSE,
    trashed_at  DATETIME,
    created_at  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (parent_id) REFERENCES files(id),
    FOREIGN KEY (owner_id)  REFERENCES users(id),
    INDEX idx_files_owner_parent   (owner_id, parent_id, is_trashed),
    INDEX idx_files_owner_starred  (owner_id, is_starred),
    INDEX idx_files_owner_trashed  (owner_id, is_trashed),
    INDEX idx_files_name_fulltext  (name)  -- 검색용
);
```

### file_shares
```sql
CREATE TABLE file_shares (
    id                    BIGINT        PRIMARY KEY AUTO_INCREMENT,
    file_id               BIGINT        NOT NULL,
    owner_id              BIGINT        NOT NULL,
    shared_with_user_id   BIGINT,                  -- NULL = 링크 공유
    share_token           VARCHAR(100)  UNIQUE,    -- NULL = 유저 공유
    permission            ENUM('VIEWER','EDITOR') NOT NULL,
    expires_at            DATETIME,
    created_at            DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (file_id)             REFERENCES files(id),
    FOREIGN KEY (owner_id)            REFERENCES users(id),
    FOREIGN KEY (shared_with_user_id) REFERENCES users(id),
    INDEX idx_shares_file         (file_id),
    INDEX idx_shares_with_user    (shared_with_user_id),
    INDEX idx_shares_token        (share_token)
);
```

### file_versions
```sql
CREATE TABLE file_versions (
    id              BIGINT  PRIMARY KEY AUTO_INCREMENT,
    file_id         BIGINT  NOT NULL,
    version_number  INT     NOT NULL,
    s3_key          VARCHAR(500) NOT NULL,
    size            BIGINT  NOT NULL,
    created_by      BIGINT  NOT NULL,
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (file_id)    REFERENCES files(id),
    FOREIGN KEY (created_by) REFERENCES users(id),
    UNIQUE KEY uk_file_version (file_id, version_number)
);
```

### file_accesses (최근 문서함)
```sql
CREATE TABLE file_accesses (
    id          BIGINT   PRIMARY KEY AUTO_INCREMENT,
    file_id     BIGINT   NOT NULL,
    user_id     BIGINT   NOT NULL,
    accessed_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (file_id)  REFERENCES files(id),
    FOREIGN KEY (user_id)  REFERENCES users(id),
    UNIQUE KEY uk_file_user (file_id, user_id),  -- 파일당 사용자 1행
    INDEX idx_access_user_time (user_id, accessed_at DESC)
);
```

## 자주 쓰는 JPA Repository 쿼리

### 파일 목록 (is_trashed 제외)
```java
List<File> findByOwnerIdAndParentIsNullAndTrashedFalse(Long ownerId);
List<File> findByOwnerIdAndParentIdAndTrashedFalse(Long ownerId, Long parentId);
```

### 최근 문서함 (accessed_at 최신순)
```java
@Query("SELECT fa FROM FileAccess fa JOIN FETCH fa.file f " +
       "WHERE fa.user.id = :userId AND f.trashed = false " +
       "ORDER BY fa.accessedAt DESC")
List<FileAccess> findRecentByUserId(@Param("userId") Long userId, Pageable pageable);
```

### 공유받은 파일 (shared_with_user_id 기준)
```java
@Query("SELECT fs FROM FileShare fs JOIN FETCH fs.file f " +
       "WHERE fs.sharedWithUser.id = :userId AND f.trashed = false " +
       "AND (fs.expiresAt IS NULL OR fs.expiresAt > CURRENT_TIMESTAMP)")
List<FileShare> findSharedWithUser(@Param("userId") Long userId);
```

### 파일 검색
```java
@Query("SELECT f FROM File f WHERE f.owner.id = :ownerId " +
       "AND LOWER(f.name) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
       "AND f.trashed = false")
List<File> searchByName(@Param("ownerId") Long ownerId, @Param("keyword") String keyword);
```

### 폴더 용량 계산 (재귀)
```java
@Query(value = "WITH RECURSIVE folder_tree AS (" +
               "  SELECT id FROM files WHERE id = :folderId " +
               "  UNION ALL " +
               "  SELECT f.id FROM files f JOIN folder_tree ft ON f.parent_id = ft.id" +
               ") " +
               "SELECT COALESCE(SUM(size), 0) FROM files WHERE id IN (SELECT id FROM folder_tree) " +
               "AND item_type = 'FILE'",
       nativeQuery = true)
Long calculateFolderSize(@Param("folderId") Long folderId);
```

## 인덱스 전략
- `(owner_id, parent_id, is_trashed)` — 폴더 내 목록 조회 핵심
- `(owner_id, is_starred)` — 별표 문서함
- `(owner_id, is_trashed)` — 휴지통
- `(user_id, accessed_at DESC)` — 최근 문서함 시간순

## JPA N+1 방지
- `@ManyToOne(fetch = FetchType.LAZY)` 기본 설정
- 목록 조회: `JOIN FETCH` 또는 `@EntityGraph` 사용
- 배치 조회: `@BatchSize(size = 100)`

## 작업 시 체크리스트
1. `application.yaml`의 `ddl-auto`가 개발 중에는 `update` 또는 `create-drop`인지 확인
2. FK 제약조건과 ON DELETE 정책 결정
3. 목록 조회 쿼리에 `is_trashed = false` 조건 반드시 포함
4. 대용량 목록은 Pageable 추가 고려
5. 파일 삭제 시 `storage_used` 업데이트 트리거 여부 결정
