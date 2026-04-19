package demo.drive.common.exception

import java.time.Instant

data class ErrorResponse(
    val status: Int,
    val code: String,
    val message: String,
    val timestamp: Instant = Instant.now(),
)
