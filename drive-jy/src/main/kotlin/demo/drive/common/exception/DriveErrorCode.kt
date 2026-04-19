package demo.drive.common.exception

import org.springframework.http.HttpStatus

enum class DriveErrorCode(
    val httpStatus: HttpStatus,
    val code: String,
    val message: String,
) {
    // ── User ──────────────────────────────────────────────────────────────
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "USER_NOT_FOUND",
        "사용자를 찾을 수 없습니다."),
    USER_EMAIL_DUPLICATE(HttpStatus.BAD_REQUEST, "USER_EMAIL_DUPLICATE",
        "이미 사용 중인 이메일입니다."),

    // ── File ──────────────────────────────────────────────────────────────
    FILE_NOT_FOUND(HttpStatus.NOT_FOUND, "FILE_NOT_FOUND",
        "파일을 찾을 수 없습니다."),
    FOLDER_NOT_FOUND(HttpStatus.NOT_FOUND, "FOLDER_NOT_FOUND",
        "폴더를 찾을 수 없습니다."),
    FILE_NOT_IN_TRASH(HttpStatus.BAD_REQUEST, "FILE_NOT_IN_TRASH",
        "휴지통에 있는 파일만 영구 삭제할 수 있습니다."),

    // ── Permission ────────────────────────────────────────────────────────
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "ACCESS_DENIED",
        "접근 권한이 없습니다."),
    PERMISSION_REQUIRED(HttpStatus.FORBIDDEN, "PERMISSION_REQUIRED",
        "해당 작업에 대한 권한이 없습니다."),
    PERMISSION_TARGET_INVALID(HttpStatus.BAD_REQUEST, "PERMISSION_TARGET_INVALID",
        "대상(user/group/token) 중 정확히 하나만 지정해야 합니다."),

    // ── Share ─────────────────────────────────────────────────────────────
    SHARE_TOKEN_INVALID(HttpStatus.NOT_FOUND, "SHARE_TOKEN_INVALID",
        "유효하지 않거나 만료된 공유 링크입니다."),

    // ── Group ─────────────────────────────────────────────────────────────
    GROUP_NOT_FOUND(HttpStatus.NOT_FOUND, "GROUP_NOT_FOUND",
        "그룹을 찾을 수 없습니다."),
    GROUP_MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "GROUP_MEMBER_NOT_FOUND",
        "그룹 멤버를 찾을 수 없습니다."),
    GROUP_MUST_HAVE_OWNER(HttpStatus.BAD_REQUEST, "GROUP_MUST_HAVE_OWNER",
        "그룹에는 최소 한 명의 소유자가 있어야 합니다."),
    INVITATION_NOT_FOUND(HttpStatus.NOT_FOUND, "INVITATION_NOT_FOUND",
        "초대를 찾을 수 없습니다."),
    INVITATION_ALREADY_PROCESSED(HttpStatus.BAD_REQUEST, "INVITATION_ALREADY_PROCESSED",
        "이미 처리된 초대입니다."),

    // ── Storage ───────────────────────────────────────────────────────────
    STORAGE_LIMIT_EXCEEDED(HttpStatus.PAYLOAD_TOO_LARGE, "STORAGE_LIMIT_EXCEEDED",
        "스토리지 용량이 초과되었습니다."),

    // ── General ───────────────────────────────────────────────────────────
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "INVALID_REQUEST",
        "잘못된 요청입니다."),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR",
        "서버 오류가 발생했습니다."),
}
