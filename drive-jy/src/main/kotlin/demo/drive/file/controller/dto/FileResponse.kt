package demo.drive.file.controller.dto

import demo.drive.file.domain.File
import demo.drive.file.domain.FileType

data class FileResponse(
    val id: Long,
    val name: String,
    val type: FileType,
    val mimeType: String?,
    val sizeBytes: Long,
    val parentId: Long?,
    val ownerId: Long,
    val isTrashed: Boolean,
    val isStarred: Boolean,
    val iconName: String,
) {
    companion object {
        fun from(file: File): FileResponse = FileResponse(
            id = file.id,
            name = file.name,
            type = file.type,
            mimeType = file.mimeType,
            sizeBytes = file.sizeBytes,
            parentId = file.parentId,
            ownerId = file.ownerId,
            isTrashed = file.isTrashed,
            isStarred = file.isStarred,
            iconName = file.iconName,
        )
    }
}
