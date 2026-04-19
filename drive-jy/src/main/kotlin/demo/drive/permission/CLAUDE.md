# permission 도메인

## 책임

파일/폴더에 대한 사용자·그룹·링크 권한 부여·수정·회수, 권한 판단 로직(`resolvePermission`).

## 파일 구성

```
permission/
├── controller/
│   ├── PermissionController.kt   GET/POST /api/permissions/file/{id}
│   │                             PATCH/DELETE /api/permissions/{permId}
│   └── dto/
│       ├── GrantPermissionRequest.kt
│       ├── UpdatePermissionRequest.kt
│       └── PermissionResponse.kt
├── service/
│   └── PermissionService.kt      resolvePermission, requirePermission
├── domain/
│   ├── FilePermission.kt         엔티티 + 팩토리 메서드
│   ├── Permission.kt             enum: OWNER, EDITOR, VIEWER (Comparable)
│   └── PermissionRepository.kt   인터페이스
└── infrastructure/
    └── PermissionJpaRepository.kt
```

## Permission enum

```kotlin
enum class Permission : Comparable<Permission> {
    VIEWER, EDITOR, OWNER;   // 순서가 곧 우선순위 (VIEWER < EDITOR < OWNER)
}
```

## FilePermission 엔티티

```kotlin
@Entity @Table(name = "file_permissions")
class FilePermission private constructor(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(name = "file_id", nullable = false)
    val fileId: Long,

    @Column(name = "granted_by_id", nullable = false)
    val grantedById: Long,

    // 대상: 셋 중 정확히 하나만 NOT NULL (도메인에서 검증)
    @Column(name = "user_id")
    val userId: Long? = null,

    @Column(name = "group_id")
    val groupId: Long? = null,

    @Column(name = "share_token", unique = true)
    val shareToken: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var permission: Permission,

    @Column(name = "inherit_to_children", nullable = false)
    var inheritToChildren: Boolean = true,

    @Column(name = "expires_at")
    var expiresAt: Instant? = null,
) {
    init {
        // DB CHECK 제약 대신 도메인에서 검증 (H2/PostgreSQL 호환)
        val count = listOfNotNull(userId, groupId, shareToken).size
        require(count == 1) { "대상(user/group/token) 중 정확히 하나만 지정해야 합니다." }
    }

    companion object {
        fun forUser(fileId: Long, grantedById: Long, userId: Long, permission: Permission, ...) =
            FilePermission(fileId = fileId, grantedById = grantedById, userId = userId, permission = permission, ...)

        fun forGroup(fileId: Long, grantedById: Long, groupId: Long, permission: Permission, ...) =
            FilePermission(fileId = fileId, grantedById = grantedById, groupId = groupId, permission = permission, ...)

        fun forLink(fileId: Long, grantedById: Long, token: String, permission: Permission, expiresAt: Instant?) =
            FilePermission(fileId = fileId, grantedById = grantedById, shareToken = token, permission = permission, expiresAt = expiresAt)
    }
}
```

## PermissionService — 핵심 권한 판단 로직

```kotlin
@Service
@Transactional(readOnly = true)
class PermissionService(
    private val fileRepository: FileRepository,
    private val permissionRepository: PermissionRepository,
    private val groupMemberRepository: GroupMemberRepository,
) {
    /**
     * 파일에 대해 userId가 갖는 최종 권한 반환.
     * 없으면 null (= 403 처리)
     */
    fun resolvePermission(fileId: Long, userId: Long): Permission? {
        val file = fileRepository.findById(fileId) ?: return null

        // 1. 소유자 → OWNER 즉시 반환
        if (file.ownerId == userId) return Permission.OWNER

        // 2. 만료되지 않은 직접 부여 권한
        val direct = permissionRepository
            .findActiveByFileIdAndUserId(fileId, userId)?.permission

        // 3. 사용자가 속한 그룹 경유 권한 (가장 높은 것)
        val myGroupIds = groupMemberRepository.findGroupIdsByUserId(userId)
        val viaGroup = if (myGroupIds.isEmpty()) null else
            permissionRepository
                .findActiveByFileIdAndGroupIdIn(fileId, myGroupIds)
                .maxOfOrNull { it.permission }

        // 4. 직접·그룹 중 높은 것 선택
        val effective = listOfNotNull(direct, viaGroup).maxOrNull()

        // 5. 없으면 부모 폴더 상속 확인 (재귀)
        if (effective == null && file.parentId != null) {
            return resolveInheritedPermission(file.parentId!!, userId, myGroupIds)
        }

        return effective
    }

    private fun resolveInheritedPermission(
        folderId: Long, userId: Long, groupIds: List<Long>
    ): Permission? {
        val folder = fileRepository.findById(folderId) ?: return null
        if (folder.ownerId == userId) return Permission.OWNER

        val inherited = permissionRepository
            .findInheritableByFileIdAndTargets(folderId, userId, groupIds)
            .maxOfOrNull { it.permission }

        return inherited ?: folder.parentId?.let { resolveInheritedPermission(it, userId, groupIds) }
    }

    /**
     * required 이상 권한이 없으면 DriveException.AccessDenied 던지기
     */
    fun requirePermission(fileId: Long, userId: Long, required: Permission) {
        val actual = resolvePermission(fileId, userId)
            ?: throw DriveException.accessDenied()
        if (actual < required)
            throw DriveException.accessDenied("${required.name} 권한이 필요합니다.")
    }
}
```

## PermissionRepository 인터페이스 주요 메서드

```kotlin
interface PermissionRepository {
    fun findActiveByFileIdAndUserId(fileId: Long, userId: Long): FilePermission?
        // WHERE file_id = ? AND user_id = ? AND (expires_at IS NULL OR expires_at > NOW())

    fun findActiveByFileIdAndGroupIdIn(fileId: Long, groupIds: List<Long>): List<FilePermission>
        // WHERE file_id = ? AND group_id IN (?) AND (expires_at IS NULL OR expires_at > NOW())

    fun findInheritableByFileIdAndTargets(folderId: Long, userId: Long, groupIds: List<Long>): List<FilePermission>
        // WHERE file_id = ? AND inherit_to_children = TRUE
        //   AND (user_id = ? OR group_id IN (?))
        //   AND (expires_at IS NULL OR expires_at > NOW())

    fun findByShareToken(token: String): FilePermission?
        // WHERE share_token = ? AND (expires_at IS NULL OR expires_at > NOW())

    fun findAllByFileId(fileId: Long): List<FilePermission>  // 권한 목록 조회용
    fun save(permission: FilePermission): FilePermission
    fun delete(permission: FilePermission)
}
```

## 권한 부여 규칙

- `OWNER` 권한은 `file_permissions`에 저장하지 않는다. `files.owner_id`로만 관리.
- 권한 부여는 파일의 OWNER만 가능.
- 만료된 권한(`expires_at < NOW()`)은 조회 시 무시.
- 공유 링크 토큰은 `SecureRandom`으로 64자 hex 생성:
  ```kotlin
  fun generateShareToken(): String {
      val bytes = ByteArray(32)
      SecureRandom().nextBytes(bytes)
      return bytes.joinToString("") { "%02x".format(it) }
  }
  ```

## 규칙

- 모든 파일 접근 서비스 메서드는 진입 시 `requirePermission` 호출 필수.
- 권한 판단 시 만료 여부를 항상 확인 (`expires_at`).
- `resolvePermission`은 읽기 전용 트랜잭션으로 실행 (`@Transactional(readOnly = true)`).
- 폴더 권한 상속 재귀는 최대 깊이에 주의 — 깊이가 20단계를 넘으면 상속 탐색을 중단하고 null 반환.
