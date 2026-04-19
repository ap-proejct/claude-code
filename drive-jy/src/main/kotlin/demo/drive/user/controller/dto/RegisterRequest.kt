package demo.drive.user.controller.dto

data class RegisterRequest(
    val email: String = "",
    val password: String = "",
    val name: String = "",
)
