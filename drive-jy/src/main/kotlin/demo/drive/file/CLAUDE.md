# file 도메인

## 책임

파일·폴더 CRUD, 업로드/다운로드, 로컬 스토리지 저장, 폴더 트리 탐색.

## 파일 구성

```
file/
├── controller/
│   ├── FileController.kt       GET /drive, /drive/folder/{id}, /drive/file/{id}/download|preview
│   │                           POST/PATCH/DELETE /api/files/**
│   ├── FolderController.kt     POST /api/folders
│   └── dto/
│       ├── FileResponse.kt
│       ├── UploadRequest.kt
│       ├── RenameRequest.kt
│       └── MoveRequest.kt
├── service/
│   ├── FileService.kt
│   └── FolderService.kt
├── domain/
│   ├── File.kt                 도메인 엔티티 (파일+폴더 통합)
│   ├── FileType.kt             enum: FILE, FOLDER
│   ├── FileRepository.kt       인터페이스
│   └── StorageService.kt       파일 저장 인터페이스
└── infrastructure/
    ├── FileJpaRepository.kt    Spring Data JPA 구현체
    └── LocalStorageService.kt  로컬 디스크 저장 구현
```

## File 엔티티 핵심 필드

```kotlin
@Entity @Table(name = "files")
class File(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(name = "owner_id", nullable = false)
    val ownerId: Long,

    @Column(name = "parent_id")
    var parentId: Long? = null,         // null = 루트

    @Column(nullable = false)
    var name: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val type: FileType,                 // FILE | FOLDER

    @Column(name = "mime_type")
    val mimeType: String? = null,       // 폴더는 null

    @Column(name = "size_bytes", nullable = false)
    var sizeBytes: Long = 0L,

    @Column(name = "storage_path")
    val storagePath: String? = null,    // 폴더는 null, 파일은 "userId/uuid.ext"

    @Column(name = "is_trashed", nullable = false)
    var isTrashed: Boolean = false,

    @Column(name = "trashed_at")
    var trashedAt: Instant? = null,
)
```

## StorageService 인터페이스

```kotlin
// domain/StorageService.kt
interface StorageService {
    fun store(userId: Long, filename: String, inputStream: InputStream, size: Long): String  // storagePath 반환
    fun load(storagePath: String): Resource
    fun delete(storagePath: String)
}
```

`LocalStorageService`는 `./storage/{userId}/{uuid}.{ext}` 경로에 저장.

## FileRepository 인터페이스 주요 메서드

```kotlin
interface FileRepository {
    fun findById(id: Long): File?
    fun findByOwnerIdAndParentIdAndIsTrashedFalse(ownerId: Long, parentId: Long?): List<File>
    fun findByParentId(parentId: Long): List<File>   // 폴더 삭제 재귀 탐색용
    fun save(file: File): File
    fun delete(file: File)
}
```

## FileService 핵심 로직

### 업로드

```kotlin
fun upload(multipartFile: MultipartFile, parentId: Long?, userId: Long): File {
    // 1. 스토리지 용량 확인
    userService.checkStorageAvailable(userId, multipartFile.size)
    // 2. 부모 폴더 권한 확인 (parentId != null 이면 EDITOR 이상 필요)
    parentId?.let { permissionService.requirePermission(it, userId, Permission.EDITOR) }
    // 3. 디스크 저장
    val storagePath = storageService.store(userId, multipartFile.originalFilename!!, ...)
    // 4. DB 저장
    val file = fileRepository.save(File(ownerId = userId, parentId = parentId, storagePath = storagePath, ...))
    // 5. 사용량 갱신
    userService.addStorageUsage(userId, multipartFile.size)
    return file
}
```

### 폴더 삭제 (재귀)

```kotlin
fun deleteRecursively(fileId: Long, userId: Long) {
    val file = fileRepository.findById(fileId) ?: return
    permissionService.requirePermission(fileId, userId, Permission.OWNER)

    if (file.type == FileType.FOLDER) {
        fileRepository.findByParentId(fileId).forEach {
            deleteRecursively(it.id, userId)
        }
    } else {
        file.storagePath?.let { storageService.delete(it) }
        userService.subtractStorageUsage(userId, file.sizeBytes)
    }
    fileRepository.delete(file)
}
```

### 다운로드

`Content-Disposition: attachment; filename="파일명"` 헤더 필수.  
한글 파일명은 RFC 5987 인코딩 적용.

```kotlin
response.setHeader(
    "Content-Disposition",
    "attachment; filename*=UTF-8''${URLEncoder.encode(file.name, "UTF-8").replace("+", "%20")}"
)
```

## 파일 아이콘 매핑

MIME 타입 → 아이콘 파일명 매핑은 `File.iconName` 계산 프로퍼티로 처리:

```kotlin
val iconName: String get() = when {
    type == FileType.FOLDER          -> "folder"
    mimeType?.startsWith("image/")  == true -> "image"
    mimeType?.startsWith("video/")  == true -> "video"
    mimeType?.startsWith("audio/")  == true -> "audio"
    mimeType == "application/pdf"            -> "pdf"
    mimeType?.contains("spreadsheet") == true || mimeType?.contains("excel") == true -> "sheet"
    mimeType?.contains("presentation") == true -> "slides"
    mimeType?.contains("word") == true || mimeType?.contains("document") == true -> "doc"
    else                                     -> "file"
}
```

## 규칙

- 파일 업로드 시 반드시 스토리지 용량 체크 후 디스크 저장.
- 파일 영구 삭제 시 `storageService.delete(storagePath)` 반드시 호출 (디스크 파일 누수 방지).
- 폴더 삭제는 재귀적으로 하위 파일/폴더 전부 처리.
- `parentId = null`이 루트 폴더를 의미. 루트 조회 시 `findByOwnerIdAndParentIdIsNull(ownerId)`.
- 파일 이동은 `file.parentId` 변경만으로 처리 (디스크 파일 이동 불필요).
