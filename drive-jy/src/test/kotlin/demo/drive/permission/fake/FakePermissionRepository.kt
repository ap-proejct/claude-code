package demo.drive.permission.fake

import demo.drive.permission.domain.FilePermission
import demo.drive.permission.domain.PermissionRepository
import java.lang.reflect.Field
import java.time.Instant

/**
 * PermissionRepository의 인메모리 Fake 구현체.
 * Service 단위 테스트에서 실제 PermissionService 인스턴스에 주입한다.
 */
class FakePermissionRepository : PermissionRepository {
    private val store = mutableMapOf<Long, FilePermission>()
    private var sequence = 1L

    override fun save(permission: FilePermission): FilePermission {
        val id = if (permission.id == 0L) sequence++ else permission.id
        val saved = setId(permission, id)
        store[id] = saved
        return saved
    }

    override fun findActiveByFileIdAndUserId(fileId: Long, userId: Long): FilePermission? {
        val now = Instant.now()
        return store.values.firstOrNull {
            it.fileId == fileId && it.userId == userId &&
                (it.expiresAt == null || it.expiresAt!! > now)
        }
    }

    override fun findActiveByFileIdAndGroupIdIn(fileId: Long, groupIds: List<Long>): List<FilePermission> {
        if (groupIds.isEmpty()) return emptyList()
        val now = Instant.now()
        return store.values.filter {
            it.fileId == fileId && it.groupId != null && it.groupId in groupIds &&
                (it.expiresAt == null || it.expiresAt!! > now)
        }
    }

    override fun findInheritableByFileIdAndTargets(
        folderId: Long, userId: Long, groupIds: List<Long>,
    ): List<FilePermission> {
        val now = Instant.now()
        return store.values.filter {
            it.fileId == folderId && it.inheritToChildren &&
                (it.userId == userId || (it.groupId != null && it.groupId in groupIds)) &&
                (it.expiresAt == null || it.expiresAt!! > now)
        }
    }

    override fun findByShareToken(token: String): FilePermission? {
        val now = Instant.now()
        return store.values.firstOrNull {
            it.shareToken == token && (it.expiresAt == null || it.expiresAt!! > now)
        }
    }

    override fun findAllByFileId(fileId: Long): List<FilePermission> =
        store.values.filter { it.fileId == fileId }

    override fun findFileIdsByUserId(userId: Long): List<Long> {
        val now = Instant.now()
        return store.values
            .filter { it.userId == userId && (it.expiresAt == null || it.expiresAt!! > now) }
            .map { it.fileId }
    }

    override fun delete(permission: FilePermission) {
        store.remove(permission.id)
    }

    fun clear() {
        store.clear()
        sequence = 1L
    }

    private fun setId(permission: FilePermission, id: Long): FilePermission {
        val field: Field = FilePermission::class.java.getDeclaredField("id")
        field.isAccessible = true
        field.set(permission, id)
        return permission
    }
}
