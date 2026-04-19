package demo.drive.permission.domain

interface PermissionRepository {
    fun findActiveByFileIdAndUserId(fileId: Long, userId: Long): FilePermission?
    fun findActiveByFileIdAndGroupIdIn(fileId: Long, groupIds: List<Long>): List<FilePermission>
    fun findInheritableByFileIdAndTargets(
        folderId: Long, userId: Long, groupIds: List<Long>,
    ): List<FilePermission>
    fun findByShareToken(token: String): FilePermission?
    fun findAllByFileId(fileId: Long): List<FilePermission>
    fun findFileIdsByUserId(userId: Long): List<Long>
    fun save(permission: FilePermission): FilePermission
    fun delete(permission: FilePermission)
}
