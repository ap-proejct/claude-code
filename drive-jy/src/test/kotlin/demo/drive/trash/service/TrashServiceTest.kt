package demo.drive.trash.service

import demo.drive.common.exception.DriveErrorCode
import demo.drive.common.exception.DriveException
import demo.drive.file.domain.File
import demo.drive.file.domain.FileType
import demo.drive.file.fake.FakeFileRepository
import demo.drive.file.fake.FakeStorageService
import demo.drive.file.service.FileService
import demo.drive.group.domain.GroupMemberRepository
import demo.drive.permission.domain.FilePermission
import demo.drive.permission.domain.Permission
import demo.drive.permission.fake.FakePermissionRepository
import demo.drive.permission.service.PermissionService
import demo.drive.user.domain.User
import demo.drive.user.fake.FakeUserRepository
import demo.drive.user.service.UserService
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import java.time.Instant
import java.time.temporal.ChronoUnit

class TrashServiceTest {

    private val fileRepository = FakeFileRepository()
    private val storageService = FakeStorageService()
    private val permissionRepository = FakePermissionRepository()
    private val userRepository = FakeUserRepository()
    private val groupMemberRepository = object : GroupMemberRepository {
        override fun findGroupIdsByUserId(userId: Long): List<Long> = emptyList()
        override fun findByGroupId(groupId: Long) = emptyList<demo.drive.group.domain.GroupMember>()
        override fun findByGroupIdAndUserId(groupId: Long, userId: Long) = null
        override fun existsByGroupIdAndUserId(groupId: Long, userId: Long) = false
        override fun save(member: demo.drive.group.domain.GroupMember) = member
        override fun delete(member: demo.drive.group.domain.GroupMember) {}
        override fun deleteAllByGroupId(groupId: Long) {}
    }

    private val permissionService = PermissionService(fileRepository, permissionRepository, groupMemberRepository)
    private val userService = UserService(userRepository, BCryptPasswordEncoder())
    private val fileService = FileService(fileRepository, storageService, permissionService, permissionRepository, userService)
    private val trashService = TrashService(fileRepository, permissionService, fileService)

    private lateinit var owner: User
    private lateinit var otherUser: User

    @BeforeEach
    fun setUp() {
        fileRepository.clear()
        permissionRepository.clear()
        userRepository.clear()

        owner = userRepository.save(User(email = "owner@test.com", password = "pw", name = "소유자"))
        otherUser = userRepository.save(User(email = "other@test.com", password = "pw", name = "타인"))
    }

    private fun saveFile(name: String = "파일.txt", ownerId: Long = owner.id, parentId: Long? = null, type: FileType = FileType.FILE): File =
        fileRepository.save(File(name = name, ownerId = ownerId, parentId = parentId, type = type))

    @Test
    fun `moveToTrash - 소유자는 파일 휴지통 이동 가능`() {
        val file = saveFile()
        trashService.moveToTrash(file.id, owner.id)

        val updated = fileRepository.findById(file.id)!!
        assertThat(updated.isTrashed).isTrue()
        assertThat(updated.trashedAt).isNotNull()
    }

    @Test
    fun `moveToTrash - 폴더 삭제 시 하위 파일도 함께 소프트 삭제`() {
        val folder = saveFile(name = "폴더", type = FileType.FOLDER)
        val child = saveFile(name = "하위파일.txt", parentId = folder.id)

        trashService.moveToTrash(folder.id, owner.id)

        assertThat(fileRepository.findById(folder.id)!!.isTrashed).isTrue()
        assertThat(fileRepository.findById(child.id)!!.isTrashed).isTrue()
    }

    @Test
    fun `moveToTrash - 소유자가 아니면 ACCESS_DENIED`() {
        val file = saveFile()

        assertThatThrownBy { trashService.moveToTrash(file.id, otherUser.id) }
            .isInstanceOf(DriveException::class.java)
            .hasFieldOrPropertyWithValue("errorCode", DriveErrorCode.ACCESS_DENIED)
    }

    @Test
    fun `listTrashed - 소유자의 휴지통 목록 반환`() {
        val file1 = saveFile(name = "파일1.txt")
        val file2 = saveFile(name = "파일2.txt")
        trashService.moveToTrash(file1.id, owner.id)

        val trashed = trashService.listTrashed(owner.id)
        assertThat(trashed).hasSize(1)
        assertThat(trashed[0].name).isEqualTo("파일1.txt")
    }

    @Test
    fun `restore - 휴지통 파일 복구`() {
        val file = saveFile()
        trashService.moveToTrash(file.id, owner.id)
        trashService.restore(file.id, owner.id)

        val restored = fileRepository.findById(file.id)!!
        assertThat(restored.isTrashed).isFalse()
        assertThat(restored.trashedAt).isNull()
    }

    @Test
    fun `restore - 부모 폴더도 삭제된 경우 루트로 복구`() {
        val folder = saveFile(name = "폴더", type = FileType.FOLDER)
        val child = saveFile(name = "하위파일.txt", parentId = folder.id)

        trashService.moveToTrash(folder.id, owner.id)
        trashService.restore(child.id, owner.id)

        val restored = fileRepository.findById(child.id)!!
        assertThat(restored.parentId).isNull()
    }

    @Test
    fun `deletePermanently - 휴지통 파일 영구 삭제`() {
        val file = saveFile()
        trashService.moveToTrash(file.id, owner.id)
        trashService.deletePermanently(file.id, owner.id)

        assertThat(fileRepository.findById(file.id)).isNull()
    }

    @Test
    fun `deletePermanently - 휴지통이 아닌 파일은 예외`() {
        val file = saveFile()

        assertThatThrownBy { trashService.deletePermanently(file.id, owner.id) }
            .isInstanceOf(DriveException::class.java)
            .hasFieldOrPropertyWithValue("errorCode", DriveErrorCode.INVALID_REQUEST)
    }

    @Test
    fun `emptyTrash - 소유자의 휴지통 전체 비우기`() {
        val file1 = saveFile(name = "파일1.txt")
        val file2 = saveFile(name = "파일2.txt")
        trashService.moveToTrash(file1.id, owner.id)
        trashService.moveToTrash(file2.id, owner.id)

        trashService.emptyTrash(owner.id)

        assertThat(fileRepository.findById(file1.id)).isNull()
        assertThat(fileRepository.findById(file2.id)).isNull()
    }

    @Test
    fun `purgeExpired - 30일 초과 항목 영구 삭제`() {
        val file = saveFile()
        trashService.moveToTrash(file.id, owner.id)
        fileRepository.findById(file.id)!!.also {
            it.trashedAt = Instant.now().minus(31, ChronoUnit.DAYS)
            fileRepository.save(it)
        }

        val threshold = Instant.now().minus(30, ChronoUnit.DAYS)
        trashService.purgeExpired(threshold)

        assertThat(fileRepository.findById(file.id)).isNull()
    }
}
