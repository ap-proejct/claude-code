package demo.drive.trash.controller.dto

import demo.drive.file.domain.File
import demo.drive.file.domain.FileType
import java.time.Instant

data class TrashItemResponse(
    val id: Long,
    val name: String,
    val type: FileType,
    val sizeBytes: Long,
    val trashedAt: Instant?,
    val iconName: String,
) {
    companion object {
        fun from(file: File) = TrashItemResponse(
            id = file.id,
            name = file.name,
            type = file.type,
            sizeBytes = file.sizeBytes,
            trashedAt = file.trashedAt,
            iconName = file.iconName,
        )
    }
}
