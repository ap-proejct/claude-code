package demo.drive.permission.domain

import jakarta.persistence.*
import java.time.Instant

@Entity
@Table(name = "file_permissions")
class FilePermission private constructor(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(name = "file_id", nullable = false)
    val fileId: Long,

    @Column(name = "granted_by_id", nullable = false)
    val grantedById: Long,

    // 대상: 셋 중 정확히 하나만 NOT NULL (DB CHECK 대신 도메인에서 검증)
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
        val count = listOfNotNull(userId, groupId, shareToken).size
        require(count == 1) { "대상(user/group/token) 중 정확히 하나만 지정해야 합니다." }
    }

    companion object {
        fun forUser(
            fileId: Long, grantedById: Long, userId: Long,
            permission: Permission, inheritToChildren: Boolean = true, expiresAt: Instant? = null,
        ) = FilePermission(
            fileId = fileId, grantedById = grantedById, userId = userId,
            permission = permission, inheritToChildren = inheritToChildren, expiresAt = expiresAt,
        )

        fun forGroup(
            fileId: Long, grantedById: Long, groupId: Long,
            permission: Permission, inheritToChildren: Boolean = true, expiresAt: Instant? = null,
        ) = FilePermission(
            fileId = fileId, grantedById = grantedById, groupId = groupId,
            permission = permission, inheritToChildren = inheritToChildren, expiresAt = expiresAt,
        )

        fun forLink(
            fileId: Long, grantedById: Long, token: String,
            permission: Permission, expiresAt: Instant? = null,
        ) = FilePermission(
            fileId = fileId, grantedById = grantedById, shareToken = token,
            permission = permission, inheritToChildren = false, expiresAt = expiresAt,
        )
    }
}
