package demo.drive.permission.service

import demo.drive.common.exception.DriveErrorCode
import demo.drive.common.exception.DriveException
import demo.drive.file.domain.File
import demo.drive.file.domain.FileType
import demo.drive.file.fake.FakeFileRepository
import demo.drive.group.domain.GroupMemberRepository
import demo.drive.permission.domain.FilePermission
import demo.drive.permission.domain.Permission
import demo.drive.permission.fake.FakePermissionRepository
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Instant

class PermissionServiceTest {

    private val fileRepository = FakeFileRepository()
    private val permissionRepository = FakePermissionRepository()

    private var memberGroupIds: List<Long> = emptyList()
    private val groupMemberRepository = object : GroupMemberRepository {
        override fun findGroupIdsByUserId(userId: Long): List<Long> = memberGroupIds
        override fun findByGroupId(groupId: Long) = emptyList<demo.drive.group.domain.GroupMember>()
        override fun findByGroupIdAndUserId(groupId: Long, userId: Long) = null
        override fun existsByGroupIdAndUserId(groupId: Long, userId: Long) = false
        override fun save(member: demo.drive.group.domain.GroupMember) = member
        override fun delete(member: demo.drive.group.domain.GroupMember) {}
        override fun deleteAllByGroupId(groupId: Long) {}
    }

    private val permissionService = PermissionService(fileRepository, permissionRepository, groupMemberRepository)

    private val ownerId = 1L
    private val otherUserId = 2L
    private val groupId = 10L

    @BeforeEach
    fun setUp() {
        fileRepository.clear()
        permissionRepository.clear()
        memberGroupIds = emptyList()
    }

    private fun saveFile(
        name: String = "파일",
        parentId: Long? = null,
        type: FileType = FileType.FILE,
    ): File = fileRepository.save(File(name = name, ownerId = ownerId, parentId = parentId, type = type))

    @Test
    fun `소유자는 OWNER 권한 반환`() {
        val file = saveFile()
        val result = permissionService.resolvePermission(file.id, ownerId)
        assertThat(result).isEqualTo(Permission.OWNER)
    }

    @Test
    fun `직접 부여된 EDITOR 권한 반환`() {
        val file = saveFile()
        permissionRepository.save(FilePermission.forUser(file.id, ownerId, otherUserId, Permission.EDITOR))

        val result = permissionService.resolvePermission(file.id, otherUserId)
        assertThat(result).isEqualTo(Permission.EDITOR)
    }

    @Test
    fun `그룹 경유 권한 반환`() {
        val file = saveFile()
        memberGroupIds = listOf(groupId)
        permissionRepository.save(FilePermission.forGroup(file.id, ownerId, groupId, Permission.VIEWER))

        val result = permissionService.resolvePermission(file.id, otherUserId)
        assertThat(result).isEqualTo(Permission.VIEWER)
    }

    @Test
    fun `직접 권한과 그룹 권한 중 높은 것 선택`() {
        val file = saveFile()
        memberGroupIds = listOf(groupId)
        permissionRepository.save(FilePermission.forUser(file.id, ownerId, otherUserId, Permission.VIEWER))
        permissionRepository.save(FilePermission.forGroup(file.id, ownerId, groupId, Permission.EDITOR))

        val result = permissionService.resolvePermission(file.id, otherUserId)
        assertThat(result).isEqualTo(Permission.EDITOR)
    }

    @Test
    fun `만료된 권한은 무효 처리`() {
        val file = saveFile()
        val expired = Instant.now().minusSeconds(3600)
        permissionRepository.save(
            FilePermission.forUser(file.id, ownerId, otherUserId, Permission.EDITOR, expiresAt = expired)
        )

        val result = permissionService.resolvePermission(file.id, otherUserId)
        assertThat(result).isNull()
    }

    @Test
    fun `부모 폴더 권한 상속 - inheritToChildren true`() {
        val folder = saveFile(name = "폴더", type = FileType.FOLDER)
        val child = saveFile(name = "하위파일", parentId = folder.id)
        permissionRepository.save(
            FilePermission.forUser(folder.id, ownerId, otherUserId, Permission.VIEWER, inheritToChildren = true)
        )

        val result = permissionService.resolvePermission(child.id, otherUserId)
        assertThat(result).isEqualTo(Permission.VIEWER)
    }

    @Test
    fun `부모 폴더 권한 - inheritToChildren false이면 미상속`() {
        val folder = saveFile(name = "폴더", type = FileType.FOLDER)
        val child = saveFile(name = "하위파일", parentId = folder.id)
        permissionRepository.save(
            FilePermission.forUser(folder.id, ownerId, otherUserId, Permission.VIEWER, inheritToChildren = false)
        )

        val result = permissionService.resolvePermission(child.id, otherUserId)
        assertThat(result).isNull()
    }

    @Test
    fun `권한 없으면 null 반환`() {
        val file = saveFile()
        val result = permissionService.resolvePermission(file.id, otherUserId)
        assertThat(result).isNull()
    }

    @Test
    fun `존재하지 않는 파일 - null 반환`() {
        val result = permissionService.resolvePermission(999L, otherUserId)
        assertThat(result).isNull()
    }

    @Test
    fun `requirePermission - 충분한 권한이면 통과`() {
        val file = saveFile()
        permissionRepository.save(FilePermission.forUser(file.id, ownerId, otherUserId, Permission.EDITOR))

        permissionService.requirePermission(file.id, otherUserId, Permission.VIEWER)
        permissionService.requirePermission(file.id, otherUserId, Permission.EDITOR)
    }

    @Test
    fun `requirePermission - 권한 없으면 ACCESS_DENIED`() {
        val file = saveFile()

        assertThatThrownBy { permissionService.requirePermission(file.id, otherUserId, Permission.VIEWER) }
            .isInstanceOf(DriveException::class.java)
            .hasFieldOrPropertyWithValue("errorCode", DriveErrorCode.ACCESS_DENIED)
    }

    @Test
    fun `requirePermission - 권한 부족하면 PERMISSION_REQUIRED`() {
        val file = saveFile()
        permissionRepository.save(FilePermission.forUser(file.id, ownerId, otherUserId, Permission.VIEWER))

        assertThatThrownBy { permissionService.requirePermission(file.id, otherUserId, Permission.EDITOR) }
            .isInstanceOf(DriveException::class.java)
            .hasFieldOrPropertyWithValue("errorCode", DriveErrorCode.PERMISSION_REQUIRED)
    }

    @Test
    fun `generateShareToken - 64자 hex 문자열 반환`() {
        val token = permissionService.generateShareToken()
        assertThat(token).hasSize(64)
        assertThat(token).matches("[0-9a-f]+")
    }
}
