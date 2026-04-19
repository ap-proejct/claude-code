package demo.drive.group.controller.dto

data class CreateGroupRequest(
    val name: String,
    val description: String? = null,
)
