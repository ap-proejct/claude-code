package demo.drive.share.service

import demo.drive.common.exception.DriveErrorCode
import demo.drive.common.exception.DriveException
import demo.drive.file.domain.File
import demo.drive.file.domain.FileType
import demo.drive.file.fake.FakeFileRepository
import demo.drive.group.domain.GroupMemberRepository
import demo.drive.permission.domain.Permission
import demo.drive.permission.fake.FakePermissionRepository
import demo.drive.permission.service.PermissionService
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.temporal.ChronoUnit

class ShareServiceTest {

    private val fileRepository = FakeFileRepository()
    private val permissionRepository = FakePermissionRepository()
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
    private val shareService = ShareService(fileRepository, permissionRepository, permissionService)

    private val ownerId = 1L
    private val otherId = 2L

    @BeforeEach
    fun setUp() {
        fileRepository.clear()
        permissionRepository.clear()
    }

    private fun saveFile(isTrashed: Boolean = false): File {
        val file = fileRepository.save(File(name = "파일.txt", ownerId = ownerId, type = FileType.FILE))
        if (isTrashed) { file.isTrashed = true; fileRepository.save(file) }
        return fileRepository.findById(file.id)!!
    }

    @Test
    fun `createLink - 소유자는 공유 링크 생성 가능`() {
        val file = saveFile()
        val fp = shareService.createLink(file.id, ownerId, Permission.VIEWER, null)

        assertThat(fp.shareToken).isNotNull().hasSize(64)
        assertThat(fp.permission).isEqualTo(Permission.VIEWER)
        assertThat(fp.fileId).isEqualTo(file.id)
    }

    @Test
    fun `createLink - 만료일 지정 가능`() {
        val file = saveFile()
        val expires = Instant.now().plus(7, ChronoUnit.DAYS)
        val fp = shareService.createLink(file.id, ownerId, Permission.EDITOR, expires)

        assertThat(fp.expiresAt).isNotNull()
    }

    @Test
    fun `createLink - 소유자가 아니면 ACCESS_DENIED`() {
        val file = saveFile()

        assertThatThrownBy { shareService.createLink(file.id, otherId, Permission.VIEWER, null) }
            .isInstanceOf(DriveException::class.java)
            .hasFieldOrPropertyWithValue("errorCode", DriveErrorCode.ACCESS_DENIED)
    }

    @Test
    fun `createLink - 존재하지 않는 파일이면 FILE_NOT_FOUND`() {
        assertThatThrownBy { shareService.createLink(999L, ownerId, Permission.VIEWER, null) }
            .isInstanceOf(DriveException::class.java)
            .hasFieldOrPropertyWithValue("errorCode", DriveErrorCode.FILE_NOT_FOUND)
    }

    @Test
    fun `resolveSharedFile - 유효한 토큰으로 파일 조회 성공`() {
        val file = saveFile()
        val fp = shareService.createLink(file.id, ownerId, Permission.VIEWER, null)

        val (resolvedFile, resolvedFp) = shareService.resolveSharedFile(fp.shareToken!!)

        assertThat(resolvedFile.id).isEqualTo(file.id)
        assertThat(resolvedFp.permission).isEqualTo(Permission.VIEWER)
    }

    @Test
    fun `resolveSharedFile - 잘못된 토큰이면 SHARE_TOKEN_INVALID`() {
        assertThatThrownBy { shareService.resolveSharedFile("invalid-token") }
            .isInstanceOf(DriveException::class.java)
            .hasFieldOrPropertyWithValue("errorCode", DriveErrorCode.SHARE_TOKEN_INVALID)
    }

    @Test
    fun `resolveSharedFile - 휴지통 파일은 SHARE_TOKEN_INVALID`() {
        val file = saveFile(isTrashed = true)
        val fp = shareService.createLink(file.id, ownerId, Permission.VIEWER, null)

        assertThatThrownBy { shareService.resolveSharedFile(fp.shareToken!!) }
            .isInstanceOf(DriveException::class.java)
            .hasFieldOrPropertyWithValue("errorCode", DriveErrorCode.SHARE_TOKEN_INVALID)
    }

    @Test
    fun `revokeLink - 소유자는 링크 취소 가능`() {
        val file = saveFile()
        val fp = shareService.createLink(file.id, ownerId, Permission.VIEWER, null)
        val token = fp.shareToken!!

        shareService.revokeLink(file.id, token, ownerId)

        assertThatThrownBy { shareService.resolveSharedFile(token) }
            .isInstanceOf(DriveException::class.java)
            .hasFieldOrPropertyWithValue("errorCode", DriveErrorCode.SHARE_TOKEN_INVALID)
    }

    @Test
    fun `listLinks - 파일의 공유 링크 목록 반환`() {
        val file = saveFile()
        shareService.createLink(file.id, ownerId, Permission.VIEWER, null)
        shareService.createLink(file.id, ownerId, Permission.EDITOR, null)

        val links = shareService.listLinks(file.id, ownerId)
        assertThat(links).hasSize(2)
    }
}
