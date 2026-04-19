# trash 도메인

## 책임

파일/폴더 소프트 삭제(휴지통 이동), 복구, 영구 삭제, 30일 자동 영구 삭제.

## 파일 구성

```
trash/
├── controller/
│   ├── TrashController.kt      GET /trash
│   │                           POST /api/trash/{id}/restore
│   │                           DELETE /api/trash/{id}        (영구 삭제)
│   │                           DELETE /api/trash             (전체 비우기)
│   └── dto/
│       └── TrashItemResponse.kt
├── service/
│   └── TrashService.kt
└── domain/
    └── (소프트 삭제 규칙 — File 엔티티의 isTrashed, trashedAt 필드 사용)
```

## TrashService 핵심 메서드

### 휴지통 이동 (소프트 삭제)

```kotlin
fun moveToTrash(fileId: Long, userId: Long) {
    val file = fileRepository.findById(fileId) ?: throw DriveException.notFound("파일")
    permissionService.requirePermission(fileId, userId, Permission.OWNER)

    // 폴더면 하위 항목도 모두 소프트 삭제
    markTrashedRecursively(file)
}

private fun markTrashedRecursively(file: File) {
    file.isTrashed = true
    file.trashedAt = Instant.now()
    fileRepository.save(file)

    if (file.type == FileType.FOLDER) {
        fileRepository.findByParentId(file.id).forEach { markTrashedRecursively(it) }
    }
}
```

### 복구

```kotlin
fun restore(fileId: Long, userId: Long) {
    val file = fileRepository.findById(fileId) ?: throw DriveException.notFound("파일")
    permissionService.requirePermission(fileId, userId, Permission.OWNER)

    // 부모 폴더도 이미 삭제된 경우 루트로 복구
    val parentExists = file.parentId?.let {
        val parent = fileRepository.findById(it)
        parent != null && !parent.isTrashed
    } ?: true

    if (!parentExists) file.parentId = null   // 루트로 복구

    restoreRecursively(file)
}

private fun restoreRecursively(file: File) {
    file.isTrashed = false
    file.trashedAt = null
    fileRepository.save(file)
    if (file.type == FileType.FOLDER) {
        fileRepository.findByParentId(file.id).forEach { restoreRecursively(it) }
    }
}
```

### 영구 삭제

```kotlin
fun deletePermanently(fileId: Long, userId: Long) {
    val file = fileRepository.findById(fileId) ?: throw DriveException.notFound("파일")
    require(file.isTrashed) { "휴지통에 있는 파일만 영구 삭제할 수 있습니다." }
    permissionService.requirePermission(fileId, userId, Permission.OWNER)
    // file 도메인의 deleteRecursively 위임
    fileService.deleteRecursively(fileId, userId)
}
```

### 30일 자동 영구 삭제 (스케줄러)

```kotlin
// TrashService 또는 별도 TrashScheduler.kt
@Scheduled(cron = "0 0 3 * * *")   // 매일 새벽 3시
fun purgeExpiredTrash() {
    val threshold = Instant.now().minus(30, ChronoUnit.DAYS)
    val expired = fileRepository.findByIsTrashedTrueAndTrashedAtBefore(threshold)
    expired.filter { it.parentId == null || !expired.any { p -> p.id == it.parentId } }
           .forEach { fileService.deleteRecursively(it.id, it.ownerId) }
}
```

`@EnableScheduling`은 `DriveApplication.kt` 또는 별도 `SchedulingConfig`에 선언.

## 규칙

- `moveToTrash`는 소프트 삭제 — 디스크 파일 삭제 금지, `isTrashed = true`만 처리.
- `deletePermanently`만 실제 디스크 파일과 DB 행을 삭제.
- 복구 시 부모 폴더가 삭제 상태면 루트(`parentId = null`)로 복구.
- 자동 삭제 스케줄러는 최상위 항목(부모가 이미 처리된 항목은 건너뜀)만 대상으로 호출해 중복 삭제 방지.
- 휴지통 페이지에는 소유자(`files.owner_id == 현재 사용자`)인 항목만 표시.
- 스토리지 사용량 갱신(`userService.subtractStorageUsage`)은 영구 삭제 시에만 수행.
