package demo.drive.share.infrastructure

import demo.drive.file.domain.File
import demo.drive.file.domain.FileType
import demo.drive.file.infrastructure.FileJpaRepository
import demo.drive.permission.domain.FilePermission
import demo.drive.permission.domain.Permission
import demo.drive.permission.infrastructure.PermissionJpaRepository
import demo.drive.support.DataJpaTestBase
import demo.drive.user.domain.User
import demo.drive.user.infrastructure.UserJpaRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.Instant
import java.time.temporal.ChronoUnit

class ShareJpaRepositoryTest @Autowired constructor(
    private val permissionJpaRepository: PermissionJpaRepository,
    private val fileJpaRepository: FileJpaRepository,
    private val userJpaRepository: UserJpaRepository,
) : DataJpaTestBase() {

    private fun createUser(email: String = "owner@test.com"): User =
        userJpaRepository.save(User(email = email, password = "hash", name = "테스터"))

    private fun createFile(ownerId: Long): File =
        fileJpaRepository.save(File(name = "파일.txt", ownerId = ownerId, type = FileType.FILE))

    @Test
    fun `공유 토큰으로 권한 조회 성공`() {
        val owner = createUser()
        val file = createFile(owner.id)
        val token = "testtoken1234567890abcdef"

        permissionJpaRepository.save(FilePermission.forLink(file.id, owner.id, token, Permission.VIEWER))

        val found = permissionJpaRepository.findByShareToken(token)
        assertThat(found).isNotNull()
        assertThat(found!!.shareToken).isEqualTo(token)
        assertThat(found.permission).isEqualTo(Permission.VIEWER)
    }

    @Test
    fun `만료된 공유 토큰은 조회되지 않음`() {
        val owner = createUser()
        val file = createFile(owner.id)
        val token = "expiredtoken123456789abc"
        val past = Instant.now().minus(1, ChronoUnit.HOURS)

        permissionJpaRepository.save(FilePermission.forLink(file.id, owner.id, token, Permission.VIEWER, past))

        val found = permissionJpaRepository.findByShareToken(token)
        assertThat(found).isNull()
    }

    @Test
    fun `파일의 공유 링크 전체 조회`() {
        val owner = createUser()
        val file = createFile(owner.id)

        permissionJpaRepository.save(FilePermission.forLink(file.id, owner.id, "token-aaa", Permission.VIEWER))
        permissionJpaRepository.save(FilePermission.forLink(file.id, owner.id, "token-bbb", Permission.EDITOR))

        val all = permissionJpaRepository.findAllByFileId(file.id)
        assertThat(all.filter { it.shareToken != null }).hasSize(2)
    }

    @Test
    fun `공유 링크 삭제 후 조회 불가`() {
        val owner = createUser()
        val file = createFile(owner.id)
        val token = "deletetoken123456789abc"

        val fp = permissionJpaRepository.save(FilePermission.forLink(file.id, owner.id, token, Permission.VIEWER))
        permissionJpaRepository.delete(fp)

        assertThat(permissionJpaRepository.findByShareToken(token)).isNull()
    }
}
