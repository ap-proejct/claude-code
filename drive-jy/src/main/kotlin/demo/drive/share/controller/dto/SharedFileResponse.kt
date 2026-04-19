package demo.drive.share.controller.dto

import demo.drive.file.domain.File
import demo.drive.file.domain.FileType
import demo.drive.permission.domain.Permission

data class SharedFileResponse(
    val fileId: Long,
    val name: String,
    val type: FileType,
    val sizeBytes: Long,
    val mimeType: String?,
    val permission: String,
    val iconName: String,
) {
    companion object {
        fun from(file: File, permission: Permission) = SharedFileResponse(
            fileId = file.id,
            name = file.name,
            type = file.type,
            sizeBytes = file.sizeBytes,
            mimeType = file.mimeType,
            permission = permission.name,
            iconName = file.iconName,
        )
    }
}
