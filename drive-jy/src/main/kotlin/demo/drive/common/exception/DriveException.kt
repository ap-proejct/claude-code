package demo.drive.common.exception

/**
 * 모든 비즈니스 예외의 단일 진입점.
 * errorCode 에 HTTP 상태, 에러 코드 문자열, 기본 메시지가 모두 포함되어 있어
 * GlobalExceptionHandler 는 errorCode 만 보면 된다.
 *
 * 사용 예:
 *   throw DriveException(DriveErrorCode.FILE_NOT_FOUND)
 *   throw DriveException(DriveErrorCode.PERMISSION_REQUIRED, "EDITOR 권한이 필요합니다.")
 *   throw DriveException.accessDenied()
 */
class DriveException(
    val errorCode: DriveErrorCode,
    message: String = errorCode.message,
) : RuntimeException(message) {

    companion object {
        fun accessDenied() =
            DriveException(DriveErrorCode.ACCESS_DENIED)

        fun accessDenied(action: String) =
            DriveException(DriveErrorCode.ACCESS_DENIED, "$action 권한이 없습니다.")

        fun permissionRequired(level: String) =
            DriveException(DriveErrorCode.PERMISSION_REQUIRED, "$level 권한이 필요합니다.")

        fun invalidRequest(message: String) =
            DriveException(DriveErrorCode.INVALID_REQUEST, message)
    }
}
