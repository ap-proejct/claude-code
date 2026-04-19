

# 백엔드 가이드

## Clean Architecture 레이어 규칙

### controller/
- HTTP 요청/응답만 담당. 비즈니스 로직 금지.
- 요청은 `Request` DTO로 받고, 응답은 `Response` DTO로 반환.
- 도메인 엔티티를 직접 응답 바디로 노출하지 않는다.
- 페이지 렌더링: `@Controller` + `Model` + Thymeleaf 템플릿명 반환.
- REST API: `@RestController` + `ResponseEntity<Response>` 반환.

```kotlin
// 올바른 예
@PostMapping("/api/files/{id}")
fun rename(@PathVariable id: Long, @RequestBody req: RenameRequest): ResponseEntity<FileResponse> {
    val file = fileService.rename(id, req.name, currentUserId())
    return ResponseEntity.ok(FileResponse.from(file))
}
```

### service/
- `@Service` + `@Transactional` 클래스 레벨 적용.
- 읽기 전용 메서드는 `@Transactional(readOnly = true)` 별도 지정.
- Repository 인터페이스(domain 레이어)만 의존 — JPA 구현체 직접 주입 금지.
- 권한 체크는 서비스 진입 시 가장 먼저 수행.
       
```kotlin
@Service
@Transactional
class FileService(
    private val fileRepository: FileRepository,         // domain 인터페이스
    private val permissionService: PermissionService,
) {
    @Transactional(readOnly = true)
    fun getFile(fileId: Long, userId: Long): File {
        permissionService.requirePermission(fileId, userId, Permission.VIEWER)
        return fileRepository.findById(fileId) ?: throw DriveException.notFound("파일")
    }
}
```

### domain/
- 순수 Kotlin 클래스. JPA 어노테이션은 허용하되 Spring 의존성은 금지.
- Repository는 **인터페이스**만 정의. 구현은 infrastructure에.
- 도메인 규칙(불변식)은 도메인 클래스 내부 메서드로 표현.

```kotlin
// domain/FileRepository.kt — 인터페이스만
interface FileRepository {
    fun findById(id: Long): File?
    fun findByParentId(parentId: Long): List<File>
    fun save(file: File): File
    fun delete(file: File)
}
```

### infrastructure/
- `FileRepository` 인터페이스를 구현하는 어댑터 클래스.
- JPA `@Entity`, Spring Data JPA `JpaRepository` 사용은 여기서만.
- 외부 의존성(파일 시스템, S3 등)은 모두 이 레이어에 격리.

```kotlin
// infrastructure/FileJpaRepository.kt
@Repository
class FileJpaRepository(
    private val jpaRepo: FileSpringDataRepository,  // Spring Data JPA
) : FileRepository {
    override fun findById(id: Long) = jpaRepo.findById(id).orElse(null)
    override fun save(file: File) = jpaRepo.save(file)
}
```

---

## DB 전환: H2 (dev) → PostgreSQL (prod)

- 프로파일: `application-dev.yaml`(H2) / `application-prod.yaml`(PostgreSQL) 분리
- 기본 프로파일 `dev`, 운영 시 `SPRING_PROFILES_ACTIVE=prod`
- `build.gradle.kts`에 `runtimeOnly("org.postgresql:postgresql")` 추가 필요

### H2 / PostgreSQL 호환 주의사항

| 구분 | H2 | PostgreSQL | 대응 방법 |
|------|----|-----------|----|
| Boolean 리터럴 | `TRUE` / `FALSE` | `TRUE` / `FALSE` | 동일, 문제없음 |
| CHECK 제약 캐스팅 | `(col IS NOT NULL)` | `(col IS NOT NULL)::INT` | JPA `@Check` 대신 서비스 레이어 검증으로 대체 |
| Auto Increment | `AUTO_INCREMENT` | `SERIAL` / `GENERATED` | JPA `GenerationType.IDENTITY` 사용 시 자동 처리 |
| 문자열 함수 | `CONCAT` | `||` 또는 `CONCAT` | JPQL `CONCAT()` 사용 |
| 페이지네이션 | `LIMIT` / `OFFSET` | `LIMIT` / `OFFSET` | Spring Data Pageable 사용 시 자동 처리 |

> `file_permissions`의 `CHECK (user_id XOR group_id XOR share_token)` 제약은  
> DB CHECK 대신 `FilePermission` 도메인 클래스 생성자에서 검증한다.

```kotlin
// domain/FilePermission.kt
class FilePermission private constructor(...) {
    init {
        val targetCount = listOfNotNull(userId, groupId, shareToken).size
        require(targetCount == 1) { "대상(user/group/token) 중 정확히 하나만 지정해야 합니다." }
    }
    companion object {
        fun forUser(...) = FilePermission(userId = userId, ...)
        fun forGroup(...) = FilePermission(groupId = groupId, ...)
        fun forLink(...) = FilePermission(shareToken = shareToken, ...)
    }
}
```

---

## Kotlin 공통 규칙

- Jakarta EE 네임스페이스 사용 (`jakarta.persistence.*`, `jakarta.servlet.*`)
- `SecurityFilterChain` 빈 방식으로 Security 설정 (WebSecurityConfigurerAdapter 사용 금지)
- `!!` 연산자 금지 → `?: throw DriveException(DriveErrorCode.XXX)` 패턴 사용
- enum은 반드시 별도 파일로 분리
- `data class`는 도메인 값 객체와 DTO에 사용, JPA 엔티티에는 일반 `class` 사용

### 현재 사용자 가져오기

```kotlin
// common/extension/SecurityExtensions.kt
fun currentUserId(): Long =
    (SecurityContextHolder.getContext().authentication.principal as UserPrincipal).id
```

### 예외 처리

`DriveException(DriveErrorCode)` 단일 클래스. errorCode에 HTTP 상태·코드 문자열·기본 메시지가 포함된다.

```kotlin
// 던지기
throw DriveException(DriveErrorCode.FILE_NOT_FOUND)
throw DriveException(DriveErrorCode.PERMISSION_REQUIRED, "EDITOR 권한이 필요합니다.")
throw DriveException.accessDenied()           // ACCESS_DENIED shorthand
throw DriveException.invalidRequest("...")    // INVALID_REQUEST shorthand

// 조회 + 없으면 예외
val file = fileRepository.findById(id) ?: throw DriveException(DriveErrorCode.FILE_NOT_FOUND)
```

새 에러 추가: `DriveErrorCode.kt`에 enum 값 하나만 추가하면 HTTP 상태·코드·메시지가 자동 연결된다.

### REST API 응답

모든 REST API는 `CommonResponse<T>`로 래핑한다.

```kotlin
@RestController
class FileController {
    @GetMapping("/api/files/{id}")
    fun getFile(@PathVariable id: Long): ResponseEntity<CommonResponse<FileResponse>> {
        val file = fileService.getFile(id, currentUserId())
        return CommonResponse.ok(FileResponse.from(file))
    }

    @PostMapping("/api/files")
    fun upload(...): ResponseEntity<CommonResponse<FileResponse>> =
        CommonResponse.created(FileResponse.from(file))

    @DeleteMapping("/api/files/{id}")
    fun delete(@PathVariable id: Long): ResponseEntity<CommonResponse<Nothing>> {
        fileService.delete(id, currentUserId())
        return CommonResponse.noContent()
    }
}
```

응답 형태:
```json
{ "success": true, "data": {...}, "error": null, "timestamp": "..." }
{ "success": false, "data": null, "error": { "status": 404, "code": "FILE_NOT_FOUND", "message": "파일을 찾을 수 없습니다." } }
```

---

## 보안 규칙

- 비밀번호: `BCryptPasswordEncoder` 필수, 평문 저장 절대 금지
- 파일 경로: 업로드 경로에 `..` 포함 시 즉시 거부 (Path Traversal 방어)
- SQL: JPA 파라미터 바인딩만 사용, 문자열 연결로 쿼리 조합 금지
- CSRF: Thymeleaf `th:action`은 자동 포함, fetch API 호출 시 `X-CSRF-TOKEN` 헤더 전달
- 인증 불필요 URL: `/auth/**`, `/share/{token}`, `/h2-console/**`(dev만), `/css/**`, `/js/**`

---

## 파일 저장 경로 전략

```
./storage/{userId}/{uuid}.{ext}
```

- 원본 파일명 → `files.name` 컬럼에만 보관
- 디스크 저장명 → UUID 기반, `files.storage_path`에 저장
- 폴더는 `storage_path = null`

---

## URL 공개 여부

| 패턴 | 인증 필요 |
|------|-----------|
| `/auth/**` | 불필요 |
| `/share/{token}` | 불필요 |
| `/h2-console/**` | dev 환경만 허용 |
| 그 외 모든 URL | 필요 |

