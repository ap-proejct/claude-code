package demo.drive.file.service

import demo.drive.common.exception.DriveErrorCode
import demo.drive.common.exception.DriveException
import demo.drive.file.domain.File
import demo.drive.file.domain.FileType
import demo.drive.file.fake.FakeFileRepository
import demo.drive.file.fake.FakeStorageService
import demo.drive.group.domain.GroupMemberRepository
import demo.drive.permission.domain.Permission
import demo.drive.permission.domain.FilePermission
import demo.drive.permission.fake.FakePermissionRepository
import demo.drive.permission.service.PermissionService
import demo.drive.user.domain.User
import demo.drive.user.fake.FakeUserRepository
import demo.drive.user.service.UserService
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.mock.web.MockMultipartFile
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder

class FileServiceTest {

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

    private lateinit var owner: User
    private lateinit var otherUser: User

    @BeforeEach
    fun setUp() {
        fileRepository.clear()
        storageService.clear()
        permissionRepository.clear()
        userRepository.clear()

        owner = userRepository.save(User(email = "owner@test.com", password = "pw", name = "소유자"))
        otherUser = userRepository.save(User(email = "other@test.com", password = "pw", name = "다른사람"))
    }

    @Test
    fun `폴더 생성 - 루트에 폴더 생성 성공`() {
        val folder = fileService.createFolder("테스트 폴더", parentId = null, userId = owner.id)

        assertThat(folder.name).isEqualTo("테스트 폴더")
        assertThat(folder.type).isEqualTo(FileType.FOLDER)
        assertThat(folder.ownerId).isEqualTo(owner.id)
        assertThat(folder.parentId).isNull()
    }

    @Test
    fun `파일 업로드 - 성공 후 스토리지 사용량 증가`() {
        val mockFile = MockMultipartFile("file", "test.txt", "text/plain", "hello".toByteArray())
        val before = userRepository.findById(owner.id)!!.storageUsedBytes

        val uploaded = fileService.upload(mockFile, parentId = null, userId = owner.id)

        assertThat(uploaded.name).isEqualTo("test.txt")
        assertThat(uploaded.sizeBytes).isEqualTo(5L)
        assertThat(uploaded.mimeType).isEqualTo("text/plain")
        val after = userRepository.findById(owner.id)!!.storageUsedBytes
        assertThat(after).isEqualTo(before + 5L)
    }

    @Test
    fun `파일 업로드 - 스토리지 초과 시 예외`() {
        // 스토리지 용량을 거의 꽉 채운 상태
        val user = userRepository.findById(owner.id)!!
        user.storageUsedBytes = user.storageLimitBytes - 1L
        userRepository.save(user)

        val mockFile = MockMultipartFile("file", "big.bin", "application/octet-stream", ByteArray(100))

        assertThatThrownBy { fileService.upload(mockFile, parentId = null, userId = owner.id) }
            .isInstanceOf(DriveException::class.java)
            .hasFieldOrPropertyWithValue("errorCode", DriveErrorCode.STORAGE_LIMIT_EXCEEDED)
    }

    @Test
    fun `파일 목록 조회 - 루트 파일만 반환`() {
        fileService.createFolder("폴더1", parentId = null, userId = owner.id)
        val mockFile = MockMultipartFile("file", "파일.txt", "text/plain", "data".toByteArray())
        fileService.upload(mockFile, parentId = null, userId = owner.id)

        val files = fileService.listFiles(parentId = null, userId = owner.id)

        assertThat(files).hasSize(2)
    }

    @Test
    fun `이름 변경 - 소유자는 성공`() {
        val folder = fileService.createFolder("원래 이름", parentId = null, userId = owner.id)

        val renamed = fileService.rename(folder.id, "새 이름", owner.id)

        assertThat(renamed.name).isEqualTo("새 이름")
    }

    @Test
    fun `이름 변경 - 권한 없는 사용자는 예외`() {
        val folder = fileService.createFolder("폴더", parentId = null, userId = owner.id)

        assertThatThrownBy { fileService.rename(folder.id, "새 이름", otherUser.id) }
            .isInstanceOf(DriveException::class.java)
    }

    @Test
    fun `휴지통 이동 - 성공`() {
        val mockFile = MockMultipartFile("file", "test.txt", "text/plain", "data".toByteArray())
        val uploaded = fileService.upload(mockFile, parentId = null, userId = owner.id)

        val trashed = fileService.moveToTrash(uploaded.id, owner.id)

        assertThat(trashed.isTrashed).isTrue()
        assertThat(trashed.trashedAt).isNotNull()
    }

    @Test
    fun `파일 이동 - 다른 폴더로 이동 성공`() {
        val folder = fileService.createFolder("대상 폴더", parentId = null, userId = owner.id)
        val mockFile = MockMultipartFile("file", "test.txt", "text/plain", "data".toByteArray())
        val uploaded = fileService.upload(mockFile, parentId = null, userId = owner.id)

        val moved = fileService.move(uploaded.id, folder.id, owner.id)

        assertThat(moved.parentId).isEqualTo(folder.id)
    }

    @Test
    fun `폴더 이동 - 하위 폴더로 순환 이동 시 예외`() {
        val parent = fileService.createFolder("부모", parentId = null, userId = owner.id)
        val child = fileService.createFolder("자식", parentId = parent.id, userId = owner.id)

        // 부모 폴더를 자식 폴더 아래로 이동 시도
        assertThatThrownBy { fileService.move(parent.id, child.id, owner.id) }
            .isInstanceOf(DriveException::class.java)
            .hasFieldOrPropertyWithValue("errorCode", DriveErrorCode.INVALID_REQUEST)
    }

    @Test
    fun `파일 다운로드 - VIEWER 권한 있는 사용자 성공`() {
        val mockFile = MockMultipartFile("file", "test.txt", "text/plain", "hello".toByteArray())
        val uploaded = fileService.upload(mockFile, parentId = null, userId = owner.id)

        // otherUser에게 VIEWER 권한 부여
        permissionRepository.save(
            FilePermission.forUser(
                fileId = uploaded.id,
                grantedById = owner.id,
                userId = otherUser.id,
                permission = Permission.VIEWER,
            )
        )

        val (file, resource) = fileService.download(uploaded.id, otherUser.id)

        assertThat(file.name).isEqualTo("test.txt")
        assertThat(resource.isReadable).isTrue()
    }

    @Test
    fun `파일 다운로드 - 권한 없는 사용자는 예외`() {
        val mockFile = MockMultipartFile("file", "test.txt", "text/plain", "hello".toByteArray())
        val uploaded = fileService.upload(mockFile, parentId = null, userId = owner.id)

        assertThatThrownBy { fileService.download(uploaded.id, otherUser.id) }
            .isInstanceOf(DriveException::class.java)
    }
}
