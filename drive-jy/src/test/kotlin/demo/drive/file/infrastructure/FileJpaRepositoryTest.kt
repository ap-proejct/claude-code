package demo.drive.file.infrastructure

import demo.drive.file.domain.File
import demo.drive.file.domain.FileType
import demo.drive.support.DataJpaTestBase
import demo.drive.user.domain.User
import demo.drive.user.infrastructure.UserJpaRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.Instant

class FileJpaRepositoryTest @Autowired constructor(
    private val fileJpaRepository: FileJpaRepository,
    private val userJpaRepository: UserJpaRepository,
) : DataJpaTestBase() {

    private fun createUser(email: String = "test@test.com"): User =
        userJpaRepository.save(User(email = email, password = "hash", name = "테스트"))

    private fun createFile(
        name: String = "test.txt",
        ownerId: Long,
        parentId: Long? = null,
        type: FileType = FileType.FILE,
        isTrashed: Boolean = false,
    ): File = fileJpaRepository.save(
        File(name = name, ownerId = ownerId, parentId = parentId, type = type, isTrashed = isTrashed)
    )

    @Test
    fun `파일 저장 후 ID 발급`() {
        val user = createUser()
        val file = createFile(ownerId = user.id)

        assertThat(file.id).isGreaterThan(0)
    }

    @Test
    fun `소유자 루트 파일 목록 조회 - parentId IS NULL 조건`() {
        val user = createUser()
        createFile(name = "루트파일", ownerId = user.id, parentId = null)
        val folder = createFile(name = "폴더", ownerId = user.id, parentId = null, type = FileType.FOLDER)
        createFile(name = "하위파일", ownerId = user.id, parentId = folder.id)

        val rootFiles = fileJpaRepository.findByOwnerIdAndParentIdIsNullAndIsTrashedFalse(user.id)

        assertThat(rootFiles).hasSize(2)
        assertThat(rootFiles.map { it.name }).containsExactlyInAnyOrder("루트파일", "폴더")
    }

    @Test
    fun `휴지통 파일은 목록에서 제외`() {
        val user = createUser()
        createFile(name = "정상파일", ownerId = user.id)
        createFile(name = "삭제파일", ownerId = user.id, isTrashed = true)

        val rootFiles = fileJpaRepository.findByOwnerIdAndParentIdIsNullAndIsTrashedFalse(user.id)

        assertThat(rootFiles).hasSize(1)
        assertThat(rootFiles.first().name).isEqualTo("정상파일")
    }

    @Test
    fun `parentId로 하위 파일 조회`() {
        val user = createUser()
        val folder = createFile(name = "폴더", ownerId = user.id, type = FileType.FOLDER)
        createFile(name = "하위파일1", ownerId = user.id, parentId = folder.id)
        createFile(name = "하위파일2", ownerId = user.id, parentId = folder.id)

        val children = fileJpaRepository.findByParentIdAndIsTrashedFalse(folder.id)

        assertThat(children).hasSize(2)
    }

    @Test
    fun `휴지통 기간 초과 파일 조회`() {
        val user = createUser()
        val oldTrashedAt = Instant.now().minusSeconds(86400 * 31)  // 31일 전
        val file = fileJpaRepository.save(
            File(
                name = "오래된파일",
                ownerId = user.id,
                type = FileType.FILE,
                isTrashed = true,
                trashedAt = oldTrashedAt,
            )
        )

        val threshold = Instant.now().minusSeconds(86400 * 30)  // 30일 전
        val expired = fileJpaRepository.findByIsTrashedTrueAndTrashedAtBefore(threshold)

        assertThat(expired.map { it.id }).contains(file.id)
    }
}
