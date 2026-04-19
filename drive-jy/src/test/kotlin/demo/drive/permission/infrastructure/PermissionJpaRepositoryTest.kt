package demo.drive.permission.infrastructure

import demo.drive.file.domain.File
import demo.drive.file.domain.FileType
import demo.drive.file.infrastructure.FileJpaRepository
import demo.drive.permission.domain.FilePermission
import demo.drive.permission.domain.Permission
import demo.drive.support.DataJpaTestBase
import demo.drive.user.domain.User
import demo.drive.user.infrastructure.UserJpaRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.Instant

class PermissionJpaRepositoryTest @Autowired constructor(
    private val permissionJpaRepository: PermissionJpaRepository,
    private val fileJpaRepository: FileJpaRepository,
    private val userJpaRepository: UserJpaRepository,
) : DataJpaTestBase() {

    private fun createUser(email: String = "owner@test.com"): User =
        userJpaRepository.save(User(email = email, password = "hash", name = "테스터"))

    private fun createFile(ownerId: Long, type: FileType = FileType.FILE, parentId: Long? = null): File =
        fileJpaRepository.save(File(name = "파일", ownerId = ownerId, parentId = parentId, type = type))

    @Test
    fun `사용자 직접 권한 저장 및 조회`() {
        val owner = createUser()
        val grantee = createUser("grantee@test.com")
        val file = createFile(owner.id)

        permissionJpaRepository.save(FilePermission.forUser(file.id, owner.id, grantee.id, Permission.EDITOR))

        val found = permissionJpaRepository.findActiveByFileIdAndUserId(file.id, grantee.id)
        assertThat(found).isNotNull()
        assertThat(found!!.permission).isEqualTo(Permission.EDITOR)
    }

    @Test
    fun `만료된 권한은 조회되지 않음`() {
        val owner = createUser()
        val grantee = createUser("grantee@test.com")
        val file = createFile(owner.id)
        val past = Instant.now().minusSeconds(3600)

        permissionJpaRepository.save(
            FilePermission.forUser(file.id, owner.id, grantee.id, Permission.EDITOR, expiresAt = past)
        )

        val found = permissionJpaRepository.findActiveByFileIdAndUserId(file.id, grantee.id)
        assertThat(found).isNull()
    }

    @Test
    fun `그룹 권한 조회 - groupId 목록으로 검색`() {
        val owner = createUser()
        val file = createFile(owner.id)
        val groupId = 10L

        permissionJpaRepository.save(FilePermission.forGroup(file.id, owner.id, groupId, Permission.VIEWER))

        val results = permissionJpaRepository.findActiveByFileIdAndGroupIdIn(file.id, listOf(groupId))
        assertThat(results).hasSize(1)
        assertThat(results[0].permission).isEqualTo(Permission.VIEWER)
    }

    @Test
    fun `그룹 목록이 비어있으면 빈 목록 반환`() {
        val owner = createUser()
        val file = createFile(owner.id)

        val results = permissionJpaRepository.findActiveByFileIdAndGroupIdIn(file.id, emptyList())
        assertThat(results).isEmpty()
    }

    @Test
    fun `상속 가능한 권한만 반환 - inheritToChildren 조건`() {
        val owner = createUser()
        val grantee = createUser("grantee@test.com")
        val folder = createFile(owner.id, FileType.FOLDER)

        permissionJpaRepository.save(
            FilePermission.forUser(folder.id, owner.id, grantee.id, Permission.VIEWER, inheritToChildren = true)
        )
        permissionJpaRepository.save(
            FilePermission.forUser(folder.id, owner.id, grantee.id, Permission.EDITOR, inheritToChildren = false)
        )

        val results = permissionJpaRepository.findInheritableByFileIdAndTargets(folder.id, grantee.id, emptyList())
        assertThat(results).hasSize(1)
        assertThat(results[0].permission).isEqualTo(Permission.VIEWER)
    }

    @Test
    fun `공유 토큰으로 권한 조회`() {
        val owner = createUser()
        val file = createFile(owner.id)
        val token = "abc123token"

        permissionJpaRepository.save(FilePermission.forLink(file.id, owner.id, token, Permission.VIEWER))

        val found = permissionJpaRepository.findByShareToken(token)
        assertThat(found).isNotNull()
        assertThat(found!!.shareToken).isEqualTo(token)
    }

    @Test
    fun `파일의 모든 권한 조회`() {
        val owner = createUser()
        val grantee1 = createUser("g1@test.com")
        val grantee2 = createUser("g2@test.com")
        val file = createFile(owner.id)

        permissionJpaRepository.save(FilePermission.forUser(file.id, owner.id, grantee1.id, Permission.VIEWER))
        permissionJpaRepository.save(FilePermission.forUser(file.id, owner.id, grantee2.id, Permission.EDITOR))

        val results = permissionJpaRepository.findAllByFileId(file.id)
        assertThat(results).hasSize(2)
    }

    @Test
    fun `권한 삭제`() {
        val owner = createUser()
        val grantee = createUser("grantee@test.com")
        val file = createFile(owner.id)

        val permission = permissionJpaRepository.save(
            FilePermission.forUser(file.id, owner.id, grantee.id, Permission.VIEWER)
        )
        permissionJpaRepository.delete(permission)

        val found = permissionJpaRepository.findActiveByFileIdAndUserId(file.id, grantee.id)
        assertThat(found).isNull()
    }
}
