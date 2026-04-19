package demo.drive.trash.infrastructure

import demo.drive.file.domain.File
import demo.drive.file.domain.FileType
import demo.drive.file.infrastructure.FileJpaRepository
import demo.drive.support.DataJpaTestBase
import demo.drive.user.domain.User
import demo.drive.user.infrastructure.UserJpaRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.Instant
import java.time.temporal.ChronoUnit

class TrashFileJpaRepositoryTest @Autowired constructor(
    private val fileJpaRepository: FileJpaRepository,
    private val userJpaRepository: UserJpaRepository,
) : DataJpaTestBase() {

    private fun createUser(email: String = "owner@test.com"): User =
        userJpaRepository.save(User(email = email, password = "hash", name = "테스터"))

    private fun createFile(ownerId: Long, name: String = "파일.txt", isTrashed: Boolean = false, trashedAt: Instant? = null): File {
        val file = fileJpaRepository.save(File(name = name, ownerId = ownerId, type = FileType.FILE))
        if (isTrashed) {
            file.isTrashed = true
            file.trashedAt = trashedAt ?: Instant.now()
            return fileJpaRepository.save(file)
        }
        return file
    }

    @Test
    fun `findByOwnerIdAndIsTrashedTrue - 휴지통 파일만 반환`() {
        val user = createUser()
        createFile(user.id, "일반파일.txt", isTrashed = false)
        createFile(user.id, "삭제파일.txt", isTrashed = true)

        val trashed = fileJpaRepository.findByOwnerIdAndIsTrashedTrue(user.id)
        assertThat(trashed).hasSize(1)
        assertThat(trashed[0].name).isEqualTo("삭제파일.txt")
    }

    @Test
    fun `findByOwnerIdAndIsTrashedTrue - 다른 소유자 파일 포함 안 함`() {
        val user1 = createUser("user1@test.com")
        val user2 = createUser("user2@test.com")
        createFile(user1.id, "user1파일.txt", isTrashed = true)
        createFile(user2.id, "user2파일.txt", isTrashed = true)

        val trashed = fileJpaRepository.findByOwnerIdAndIsTrashedTrue(user1.id)
        assertThat(trashed).hasSize(1)
        assertThat(trashed[0].ownerId).isEqualTo(user1.id)
    }

    @Test
    fun `findByIsTrashedTrueAndTrashedAtBefore - 기준일 이전 항목만 반환`() {
        val user = createUser()
        val oldDate = Instant.now().minus(31, ChronoUnit.DAYS)
        val recentDate = Instant.now().minus(1, ChronoUnit.DAYS)

        createFile(user.id, "오래된파일.txt", isTrashed = true, trashedAt = oldDate)
        createFile(user.id, "최근파일.txt", isTrashed = true, trashedAt = recentDate)

        val threshold = Instant.now().minus(30, ChronoUnit.DAYS)
        val expired = fileJpaRepository.findByIsTrashedTrueAndTrashedAtBefore(threshold)

        assertThat(expired).hasSize(1)
        assertThat(expired[0].name).isEqualTo("오래된파일.txt")
    }

    @Test
    fun `isTrashed 및 trashedAt 필드 저장 확인`() {
        val user = createUser()
        val now = Instant.now()
        val file = createFile(user.id, isTrashed = true, trashedAt = now)

        val found = fileJpaRepository.findById(file.id)
        assertThat(found).isNotNull()
        assertThat(found!!.isTrashed).isTrue()
        assertThat(found.trashedAt).isNotNull()
    }
}
