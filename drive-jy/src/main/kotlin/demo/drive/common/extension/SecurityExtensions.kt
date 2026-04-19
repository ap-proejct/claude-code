package demo.drive.common.extension

import demo.drive.common.exception.DriveErrorCode
import demo.drive.common.exception.DriveException
import demo.drive.user.domain.UserPrincipal
import org.springframework.security.core.context.SecurityContextHolder

fun currentUserId(): Long {
    val principal = SecurityContextHolder.getContext().authentication?.principal
        ?: throw DriveException(DriveErrorCode.ACCESS_DENIED)
    return (principal as UserPrincipal).id
}
