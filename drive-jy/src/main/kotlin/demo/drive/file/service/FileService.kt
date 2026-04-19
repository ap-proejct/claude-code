package demo.drive.file.service

import demo.drive.common.exception.DriveErrorCode
import demo.drive.common.exception.DriveException
import demo.drive.file.domain.File
import demo.drive.file.domain.FileRepository
import demo.drive.file.domain.FileType
import demo.drive.file.domain.StorageService
import demo.drive.permission.domain.Permission
import demo.drive.permission.domain.PermissionRepository
import demo.drive.permission.service.PermissionService
import demo.drive.user.service.UserService
import org.springframework.core.io.Resource
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import java.time.Instant

@Service
@Transactional
class FileService(
    private val fileRepository: FileRepository,
    private val storageService: StorageService,
    private val permissionService: PermissionService,
    private val permissionRepository: PermissionRepository,
    private val userService: UserService,
) {

    @Transactional(readOnly = true)
    fun listFiles(parentId: Long?, userId: Long): List<File> {
        return if (parentId == null) {
            fileRepository.findByOwnerIdAndParentIdIsNullAndIsTrashedFalse(userId)
        } else {
            permissionService.requirePermission(parentId, userId, Permission.VIEWER)
            fileRepository.findByParentIdAndIsTrashedFalse(parentId)
        }
    }

    @Transactional(readOnly = true)
    fun getFile(fileId: Long, userId: Long): File {
        val file = fileRepository.findById(fileId) ?: throw DriveException(DriveErrorCode.FILE_NOT_FOUND)
        permissionService.requirePermission(fileId, userId, Permission.VIEWER)
        return file
    }

    fun createFolder(name: String, parentId: Long?, userId: Long): File {
        parentId?.let { permissionService.requirePermission(it, userId, Permission.EDITOR) }
        val folder = File(
            name = name,
            ownerId = userId,
            parentId = parentId,
            type = FileType.FOLDER,
        )
        return fileRepository.save(folder)
    }

    fun upload(multipartFile: MultipartFile, parentId: Long?, userId: Long): File {
        userService.checkStorageAvailable(userId, multipartFile.size)
        parentId?.let { permissionService.requirePermission(it, userId, Permission.EDITOR) }

        val originalFilename = multipartFile.originalFilename ?: "upload"
        val storagePath = storageService.store(
            userId = userId,
            filename = originalFilename,
            inputStream = multipartFile.inputStream,
            size = multipartFile.size,
        )

        val file = fileRepository.save(
            File(
                name = originalFilename,
                ownerId = userId,
                parentId = parentId,
                type = FileType.FILE,
                mimeType = multipartFile.contentType,
                storagePath = storagePath,
                sizeBytes = multipartFile.size,
            )
        )

        userService.addStorageUsage(userId, multipartFile.size)
        return file
    }

    @Transactional(readOnly = true)
    fun download(fileId: Long, userId: Long): Pair<File, Resource> {
        val file = fileRepository.findById(fileId) ?: throw DriveException(DriveErrorCode.FILE_NOT_FOUND)
        permissionService.requirePermission(fileId, userId, Permission.VIEWER)
        if (file.type == FileType.FOLDER) throw DriveException.invalidRequest("폴더는 다운로드할 수 없습니다.")
        val storagePath = file.storagePath ?: throw DriveException(DriveErrorCode.FILE_NOT_FOUND)
        return Pair(file, storageService.load(storagePath))
    }

    fun rename(fileId: Long, newName: String, userId: Long): File {
        val file = fileRepository.findById(fileId) ?: throw DriveException(DriveErrorCode.FILE_NOT_FOUND)
        permissionService.requirePermission(fileId, userId, Permission.EDITOR)
        file.name = newName
        return fileRepository.save(file)
    }

    fun move(fileId: Long, newParentId: Long?, userId: Long): File {
        val file = fileRepository.findById(fileId) ?: throw DriveException(DriveErrorCode.FILE_NOT_FOUND)
        permissionService.requirePermission(fileId, userId, Permission.EDITOR)
        newParentId?.let { permissionService.requirePermission(it, userId, Permission.EDITOR) }

        // 폴더를 자기 자신 또는 하위 폴더로 이동하는 순환 참조 방지
        if (file.type == FileType.FOLDER && newParentId != null) {
            if (isDescendant(ancestorId = fileId, targetId = newParentId)) {
                throw DriveException.invalidRequest("하위 폴더로 이동할 수 없습니다.")
            }
        }

        file.parentId = newParentId
        return fileRepository.save(file)
    }

    fun moveToTrash(fileId: Long, userId: Long): File {
        val file = fileRepository.findById(fileId) ?: throw DriveException(DriveErrorCode.FILE_NOT_FOUND)
        permissionService.requirePermission(fileId, userId, Permission.EDITOR)
        file.isTrashed = true
        file.trashedAt = Instant.now()
        return fileRepository.save(file)
    }

    fun deleteRecursively(fileId: Long, userId: Long) {
        val file = fileRepository.findById(fileId) ?: return
        permissionService.requirePermission(fileId, userId, Permission.OWNER)

        if (file.type == FileType.FOLDER) {
            fileRepository.findByParentId(fileId).forEach { deleteRecursively(it.id, userId) }
        } else {
            file.storagePath?.let { storageService.delete(it) }
            userService.subtractStorageUsage(userId, file.sizeBytes)
        }
        fileRepository.delete(file)
    }

    @Transactional(readOnly = true)
    fun listSharedWithMe(userId: Long): List<File> {
        val fileIds = permissionRepository.findFileIdsByUserId(userId)
        if (fileIds.isEmpty()) return emptyList()
        return fileRepository.findAllByIdIn(fileIds)
            .filter { !it.isTrashed && it.ownerId != userId }
    }

    @Transactional(readOnly = true)
    fun listStarred(userId: Long): List<File> =
        fileRepository.findByOwnerIdAndIsStarredTrueAndIsTrashedFalse(userId)

    fun toggleStar(fileId: Long, userId: Long): File {
        val file = fileRepository.findById(fileId) ?: throw DriveException(DriveErrorCode.FILE_NOT_FOUND)
        permissionService.requirePermission(fileId, userId, Permission.VIEWER)
        file.isStarred = !file.isStarred
        return fileRepository.save(file)
    }

    private fun isDescendant(ancestorId: Long, targetId: Long): Boolean {
        var currentId: Long? = targetId
        var depth = 0
        while (currentId != null && depth < 20) {
            if (currentId == ancestorId) return true
            currentId = fileRepository.findById(currentId)?.parentId
            depth++
        }
        return false
    }
}
