package demo.drive.common.exception

import demo.drive.common.response.CommonResponse
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.ResponseEntity
import org.springframework.security.access.AccessDeniedException
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.servlet.resource.NoResourceFoundException

@ControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(DriveException::class)
    fun handleDriveException(e: DriveException, request: HttpServletRequest, model: Model): Any =
        if (request.isApiRequest()) {
            CommonResponse.fail(e.errorCode, e.message ?: e.errorCode.message)
        } else {
            model.addAttribute("message", e.message)
            e.errorCode.httpStatus.value().toErrorView()
        }

    @ExceptionHandler(AccessDeniedException::class)
    fun handleSpringAccessDenied(e: AccessDeniedException, request: HttpServletRequest, model: Model): Any =
        handleDriveException(
            DriveException(DriveErrorCode.ACCESS_DENIED, e.message ?: DriveErrorCode.ACCESS_DENIED.message),
            request, model,
        )

    @ExceptionHandler(NoResourceFoundException::class)
    fun handleNoResource(e: NoResourceFoundException, request: HttpServletRequest, model: Model): Any =
        handleDriveException(
            DriveException(DriveErrorCode.FILE_NOT_FOUND, "요청한 경로를 찾을 수 없습니다."),
            request, model,
        )

    @ExceptionHandler(Exception::class)
    fun handleUnexpected(e: Exception, request: HttpServletRequest, model: Model): Any {
        val mapped = DriveException(DriveErrorCode.INTERNAL_ERROR)
        return if (request.isApiRequest()) {
            CommonResponse.fail(mapped.errorCode)
        } else {
            model.addAttribute("message", mapped.message)
            "error/500"
        }
    }

    private fun HttpServletRequest.isApiRequest(): Boolean = requestURI.startsWith("/api/")

    private fun Int.toErrorView(): String = when (this) {
        403, 404 -> "error/$this"
        else     -> "error/500"
    }
}
