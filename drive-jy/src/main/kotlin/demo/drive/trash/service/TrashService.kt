package demo.drive.trash.service

import demo.drive.common.exception.DriveErrorCode
import demo.drive.common.exception.DriveException
import demo.drive.file.domain.File
import demo.drive.file.domain.FileRepository
import demo.drive.file.domain.FileType
import demo.drive.file.service.FileService
import demo.drive.permission.domain.Permission
import demo.drive.permission.service.PermissionService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Service
@Transactional
class TrashService(
    private val fileRepository: FileRepository,
    private val permissionService: PermissionService,
    private val fileService: FileService,
) {
    @Transactional(readOnly = true)
    fun listTrashed(userId: Long): List<File> =
        fileRepository.findByOwnerIdAndIsTrashedTrue(userId)

    fun moveToTrash(fileId: Long, userId: Long) {
        val file = fileRepository.findById(fileId) ?: throw DriveException(DriveErrorCode.FILE_NOT_FOUND)
        permissionService.requirePermission(fileId, userId, Permission.OWNER)
        markTrashedRecursively(file)
    }

    fun restore(fileId: Long, userId: Long) {
        val file = fileRepository.findById(fileId) ?: throw DriveException(DriveErrorCode.FILE_NOT_FOUND)
        permissionService.requirePermission(fileId, userId, Permission.OWNER)

        val parentValid = file.parentId?.let { parentId ->
            val parent = fileRepository.findById(parentId)
            parent != null && !parent.isTrashed
        } ?: true

        if (!parentValid) file.parentId = null

        restoreRecursively(file)
    }

    fun deletePermanently(fileId: Long, userId: Long) {
        val file = fileRepository.findById(fileId) ?: throw DriveException(DriveErrorCode.FILE_NOT_FOUND)
        if (!file.isTrashed) throw DriveException.invalidRequest("휴지통에 있는 파일만 영구 삭제할 수 있습니다.")
        fileService.deleteRecursively(fileId, userId)
    }

    fun emptyTrash(userId: Long) {
        fileRepository.findByOwnerIdAndIsTrashedTrue(userId)
            .filter { it.parentId == null || fileRepository.findById(it.parentId!!)?.isTrashed != true }
            .forEach { fileService.deleteRecursively(it.id, userId) }
    }

    fun purgeExpired(threshold: Instant) {
        val expired = fileRepository.findByIsTrashedTrueAndTrashedAtBefore(threshold)
        expired
            .filter { it.parentId == null || expired.none { p -> p.id == it.parentId } }
            .forEach { fileService.deleteRecursively(it.id, it.ownerId) }
    }

    private fun markTrashedRecursively(file: File) {
        file.isTrashed = true
        file.trashedAt = Instant.now()
        fileRepository.save(file)
        if (file.type == FileType.FOLDER) {
            fileRepository.findByParentId(file.id).forEach { markTrashedRecursively(it) }
        }
    }

    private fun restoreRecursively(file: File) {
        file.isTrashed = false
        file.trashedAt = null
        fileRepository.save(file)
        if (file.type == FileType.FOLDER) {
            fileRepository.findByParentId(file.id).forEach { restoreRecursively(it) }
        }
    }
}
