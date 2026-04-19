package demo.drive.common.config

import demo.drive.group.service.GroupInvitationService
import demo.drive.user.domain.UserPrincipal
import demo.drive.user.service.UserService
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ModelAttribute

@ControllerAdvice
class CommonModelAdvice(
    private val userService: UserService,
    private val groupInvitationService: GroupInvitationService,
) {

    @ModelAttribute("storageUsedBytes")
    fun storageUsedBytes(auth: Authentication?): Long {
        val principal = auth?.principal as? UserPrincipal ?: return 0L
        return runCatching { userService.findById(principal.id).storageUsedBytes }.getOrDefault(0L)
    }

    @ModelAttribute("storageLimitBytes")
    fun storageLimitBytes(auth: Authentication?): Long {
        val principal = auth?.principal as? UserPrincipal ?: return 16_106_127_360L
        return runCatching { userService.findById(principal.id).storageLimitBytes }.getOrDefault(16_106_127_360L)
    }

    @ModelAttribute("storageUsedPct")
    fun storageUsedPct(auth: Authentication?): Int {
        val principal = auth?.principal as? UserPrincipal ?: return 0
        val user = runCatching { userService.findById(principal.id) }.getOrNull() ?: return 0
        if (user.storageLimitBytes == 0L) return 0
        return ((user.storageUsedBytes.toDouble() / user.storageLimitBytes) * 100).toInt().coerceIn(0, 100)
    }

    @ModelAttribute("currentUser")
    fun currentUser(auth: Authentication?): demo.drive.user.domain.User? {
        val principal = auth?.principal as? UserPrincipal ?: return null
        return runCatching { userService.findById(principal.id) }.getOrNull()
    }

    @ModelAttribute("pendingInvitationCount")
    fun pendingInvitationCount(auth: Authentication?): Long {
        val principal = auth?.principal as? UserPrincipal ?: return 0L
        return runCatching { groupInvitationService.countPending(principal.id) }.getOrDefault(0L)
    }
}
