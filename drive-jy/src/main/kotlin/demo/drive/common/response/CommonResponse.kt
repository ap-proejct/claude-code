package demo.drive.common.response

import demo.drive.common.exception.DriveErrorCode
import demo.drive.common.exception.ErrorResponse
import org.springframework.http.ResponseEntity
import java.time.Instant

/**
 * 모든 REST API 응답의 공통 래퍼.
 *
 * 성공:  CommonResponse.ok(data)
 * 생성:  CommonResponse.created(data)
 * 실패:  CommonResponse.fail(errorCode)          → GlobalExceptionHandler에서 사용
 * 빈 성공: CommonResponse.noContent()
 *
 * 응답 형태:
 * {
 *   "success": true,
 *   "data": { ... },
 *   "error": null,
 *   "timestamp": "2026-04-14T..."
 * }
 */
data class CommonResponse<T>(
    val success: Boolean,
    val data: T? = null,
    val error: ErrorResponse? = null,
    val timestamp: Instant = Instant.now(),
) {
    companion object {
        fun <T> ok(data: T): ResponseEntity<CommonResponse<T>> =
            ResponseEntity.ok(CommonResponse(success = true, data = data))

        fun <T> created(data: T): ResponseEntity<CommonResponse<T>> =
            ResponseEntity.status(201).body(CommonResponse(success = true, data = data))

        fun noContent(): ResponseEntity<CommonResponse<Nothing>> =
            ResponseEntity.status(204).body(CommonResponse(success = true))

        fun fail(errorCode: DriveErrorCode, message: String = errorCode.message): ResponseEntity<CommonResponse<Nothing>> =
            ResponseEntity.status(errorCode.httpStatus).body(
                CommonResponse(
                    success = false,
                    error = ErrorResponse(
                        status  = errorCode.httpStatus.value(),
                        code    = errorCode.code,
                        message = message,
                    ),
                )
            )
    }
}
