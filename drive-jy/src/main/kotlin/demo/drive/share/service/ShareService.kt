package demo.drive.share.service

import demo.drive.common.exception.DriveErrorCode
import demo.drive.common.exception.DriveException
import demo.drive.file.domain.File
import demo.drive.file.domain.FileRepository
import demo.drive.permission.domain.FilePermission
import demo.drive.permission.domain.Permission
import demo.drive.permission.domain.PermissionRepository
import demo.drive.permission.service.PermissionService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Service
@Transactional
class ShareService(
    private val fileRepository: FileRepository,
    private val permissionRepository: PermissionRepository,
    private val permissionService: PermissionService,
) {
    fun createLink(fileId: Long, requesterId: Long, permission: Permission, expiresAt: Instant?): FilePermission {
        fileRepository.findById(fileId) ?: throw DriveException(DriveErrorCode.FILE_NOT_FOUND)
        permissionService.requirePermission(fileId, requesterId, Permission.OWNER)

        val token = permissionService.generateShareToken()
        val fp = FilePermission.forLink(fileId, requesterId, token, permission, expiresAt)
        return permissionRepository.save(fp)
    }

    fun revokeLink(fileId: Long, token: String, requesterId: Long) {
        permissionService.requirePermission(fileId, requesterId, Permission.OWNER)
        val fp = permissionRepository.findByShareToken(token)
            ?: throw DriveException(DriveErrorCode.SHARE_TOKEN_INVALID)
        permissionRepository.delete(fp)
    }

    @Transactional(readOnly = true)
    fun resolveSharedFile(token: String): Pair<File, FilePermission> {
        val fp = permissionRepository.findByShareToken(token)
            ?: throw DriveException(DriveErrorCode.SHARE_TOKEN_INVALID)
        val file = fileRepository.findById(fp.fileId)
            ?: throw DriveException(DriveErrorCode.FILE_NOT_FOUND)
        if (file.isTrashed) throw DriveException(DriveErrorCode.SHARE_TOKEN_INVALID)
        return Pair(file, fp)
    }

    @Transactional(readOnly = true)
    fun listLinks(fileId: Long, requesterId: Long): List<FilePermission> {
        permissionService.requirePermission(fileId, requesterId, Permission.OWNER)
        return permissionRepository.findAllByFileId(fileId).filter { it.shareToken != null }
    }
}
