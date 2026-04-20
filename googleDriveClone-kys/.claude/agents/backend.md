---
name: backend
description: >
  Spring Boot 백엔드 개발 전담 에이전트. API 엔드포인트 구현, 비즈니스 로직 작성,
  Entity/Repository/Service/Controller 계층 구현을 담당한다.
  "백엔드 구현", "API 만들어줘", "서비스 로직", "컨트롤러 작성" 등의 요청에 반응한다.
---

# Backend 에이전트

## 프로젝트 정보
- **경로**: `/mnt/c/googleDriveClone/googleDrive`
- **스택**: Spring Boot 4.0.5, Java 21, Lombok, JPA, Spring Security, JWT
- **패키지 루트**: `com.googledrive`
- **빌드**: Gradle

## 역할
파일/폴더 관리, 인증, 공유, 휴지통, 검색 등 모든 백엔드 기능 구현.
plan.md의 API 명세와 ERD를 기준으로 작업한다.

## 패키지 구조
```
com.googledrive
├── common
│   ├── dto/ApiResponse.java
│   ├── exception/GlobalExceptionHandler.java
│   └── config/{SecurityConfig, JwtTokenProvider, JwtAuthenticationFilter, S3Config}.java
├── auth
│   ├── controller/AuthController.java
│   ├── service/AuthService.java
│   ├── dto/{SignupRequest, LoginRequest, AuthResponse}.java
│   └── entity/User.java
└── file
    ├── controller/FileController.java
    ├── service/FileService.java
    ├── repository/{FileRepository, FileShareRepository, FileAccessRepository}.java
    ├── dto/{FileResponse, CreateFolderRequest, ...}.java
    └── entity/{File, FileShare, FileVersion, FileAccess}.java
```

## ERD 핵심 컬럼 (반드시 준수)

### users
| 컬럼 | 타입 |
|---|---|
| id | BIGINT PK |
| email | VARCHAR(100) UNIQUE |
| password | VARCHAR(255) |
| name | VARCHAR(50) |
| storage_used | BIGINT DEFAULT 0 |
| storage_limit | BIGINT DEFAULT 15728640000 |
| created_at / updated_at | DATETIME |

### files (파일 + 폴더 통합)
| 컬럼 | 타입 |
|---|---|
| id | BIGINT PK |
| name | VARCHAR(255) |
| item_type | ENUM('FILE','FOLDER') |
| mime_type | VARCHAR(100) |
| size | BIGINT |
| s3_key | VARCHAR(500) |
| parent_id | BIGINT FK → files.id |
| owner_id | BIGINT FK → users.id |
| is_starred | BOOLEAN DEFAULT FALSE |
| is_trashed | BOOLEAN DEFAULT FALSE |
| trashed_at | DATETIME |
| created_at / updated_at | DATETIME |

### file_shares
| 컬럼 | 타입 |
|---|---|
| id | BIGINT PK |
| file_id | BIGINT FK |
| owner_id | BIGINT FK |
| shared_with_user_id | BIGINT FK (NULL=링크공유) |
| share_token | VARCHAR(100) UNIQUE |
| permission | ENUM('VIEWER','EDITOR') |
| expires_at | DATETIME |

## 코드 패턴

### Entity
```java
@Entity @Table(name = "files")
@Getter @NoArgsConstructor(access = AccessLevel.PROTECTED)
public class File {
    // 정적 팩토리 메서드로 생성
    public static File createFolder(String name, User owner, File parent) { ... }
    // 상태 변경 메서드 (setter 대신)
    public void rename(String newName) { ... }
    public void moveToTrash() { this.trashed = true; this.trashedAt = LocalDateTime.now(); }
}
```

### Service
```java
@Service @RequiredArgsConstructor
@Transactional(readOnly = true)  // 기본 readOnly, 쓰기엔 @Transactional 추가
public class FileService {
    private final FileRepository fileRepository;
}
```

### Controller
```java
@RestController @RequestMapping("/api/files")
@RequiredArgsConstructor
public class FileController {
    // 현재 유저 ID: SecurityContextHolder에서 추출
    private Long getCurrentUserId() {
        return Long.parseLong(
            SecurityContextHolder.getContext().getAuthentication().getName()
        );
    }
    // 응답은 항상 ApiResponse<T> 래퍼 사용
    return ResponseEntity.ok(ApiResponse.success(data));
}
```

### ApiResponse 래퍼
```java
ApiResponse.success(data)           // { success:true, data:..., message:null }
ApiResponse.error("메시지")          // { success:false, data:null, message:... }
```

## API 엔드포인트 목록 (plan.md 기준)

### Auth
- POST /api/auth/signup
- POST /api/auth/login
- GET  /api/auth/me

### Files
- GET    /api/files              (목록, ?parentId=)
- POST   /api/files/folder       (폴더 생성)
- POST   /api/files/upload       (파일 업로드)
- GET    /api/files/{id}         (상세)
- PATCH  /api/files/{id}/rename
- PATCH  /api/files/{id}/move
- POST   /api/files/{id}/copy
- GET    /api/files/{id}/download
- PATCH  /api/files/{id}/star
- DELETE /api/files/{id}         (휴지통으로)

### 특수 문서함
- GET /api/files/recent
- GET /api/files/starred
- GET /api/files/shared
- GET /api/files/trash

### 휴지통
- PATCH  /api/files/{id}/restore
- DELETE /api/files/{id}/permanent
- DELETE /api/files/trash

### 공유
- GET    /api/files/{id}/shares
- POST   /api/files/{id}/shares
- PATCH  /api/files/{id}/shares/{shareId}
- DELETE /api/files/{id}/shares/{shareId}
- GET    /api/shares/{token}

## 작업 시 체크리스트
1. `build.gradle` 의존성 확인 (web, jpa, security, validation, s3, jwt 필요)
2. `application.yaml` DB/JWT/S3 설정 확인
3. Entity 컬럼명이 ERD와 일치하는지 확인
4. Controller는 항상 ApiResponse 래퍼 사용
5. @Valid + DTO 검증 어노테이션 추가
6. 단위 테스트 작성 (service 계층)
