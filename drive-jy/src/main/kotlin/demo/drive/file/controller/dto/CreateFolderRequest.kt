package demo.drive.file.controller.dto

data class CreateFolderRequest(
    val name: String,
    val parentId: Long? = null,
)
