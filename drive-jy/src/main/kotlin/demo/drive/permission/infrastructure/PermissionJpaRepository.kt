package demo.drive.permission.infrastructure

import demo.drive.permission.domain.FilePermission
import demo.drive.permission.domain.PermissionRepository
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.Instant

interface PermissionSpringDataRepository : JpaRepository<FilePermission, Long> {

    @Query("""
        SELECT p FROM FilePermission p
        WHERE p.fileId = :fileId AND p.userId = :userId
          AND (p.expiresAt IS NULL OR p.expiresAt > :now)
    """)
    fun findActiveByFileIdAndUserId(
        @Param("fileId") fileId: Long,
        @Param("userId") userId: Long,
        @Param("now") now: Instant = Instant.now(),
    ): FilePermission?

    @Query("""
        SELECT p FROM FilePermission p
        WHERE p.fileId = :fileId AND p.groupId IN :groupIds
          AND (p.expiresAt IS NULL OR p.expiresAt > :now)
    """)
    fun findActiveByFileIdAndGroupIdIn(
        @Param("fileId") fileId: Long,
        @Param("groupIds") groupIds: List<Long>,
        @Param("now") now: Instant = Instant.now(),
    ): List<FilePermission>

    @Query("""
        SELECT p FROM FilePermission p
        WHERE p.fileId = :folderId AND p.inheritToChildren = TRUE
          AND (p.userId = :userId OR p.groupId IN :groupIds)
          AND (p.expiresAt IS NULL OR p.expiresAt > :now)
    """)
    fun findInheritableByFileIdAndTargets(
        @Param("folderId") folderId: Long,
        @Param("userId") userId: Long,
        @Param("groupIds") groupIds: List<Long>,
        @Param("now") now: Instant = Instant.now(),
    ): List<FilePermission>

    @Query("""
        SELECT p FROM FilePermission p
        WHERE p.shareToken = :token
          AND (p.expiresAt IS NULL OR p.expiresAt > :now)
    """)
    fun findByShareToken(
        @Param("token") token: String,
        @Param("now") now: Instant = Instant.now(),
    ): FilePermission?

    fun findAllByFileId(fileId: Long): List<FilePermission>

    @Query("""
        SELECT p.fileId FROM FilePermission p
        WHERE p.userId = :userId
          AND (p.expiresAt IS NULL OR p.expiresAt > :now)
    """)
    fun findFileIdsByUserId(
        @Param("userId") userId: Long,
        @Param("now") now: Instant = Instant.now(),
    ): List<Long>
}

@Repository
class PermissionJpaRepository(
    private val jpaRepo: PermissionSpringDataRepository,
) : PermissionRepository {

    override fun findActiveByFileIdAndUserId(fileId: Long, userId: Long): FilePermission? =
        jpaRepo.findActiveByFileIdAndUserId(fileId, userId)

    override fun findActiveByFileIdAndGroupIdIn(fileId: Long, groupIds: List<Long>): List<FilePermission> =
        if (groupIds.isEmpty()) emptyList()
        else jpaRepo.findActiveByFileIdAndGroupIdIn(fileId, groupIds)

    override fun findInheritableByFileIdAndTargets(
        folderId: Long, userId: Long, groupIds: List<Long>,
    ): List<FilePermission> =
        jpaRepo.findInheritableByFileIdAndTargets(folderId, userId, groupIds)

    override fun findByShareToken(token: String): FilePermission? =
        jpaRepo.findByShareToken(token)

    override fun findAllByFileId(fileId: Long): List<FilePermission> =
        jpaRepo.findAllByFileId(fileId)

    override fun findFileIdsByUserId(userId: Long): List<Long> =
        jpaRepo.findFileIdsByUserId(userId)

    override fun save(permission: FilePermission): FilePermission =
        jpaRepo.save(permission)

    override fun delete(permission: FilePermission) =
        jpaRepo.delete(permission)
}
