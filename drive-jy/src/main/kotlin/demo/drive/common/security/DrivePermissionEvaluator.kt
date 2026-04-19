package demo.drive.common.security

import demo.drive.permission.domain.Permission
import demo.drive.permission.service.PermissionService
import demo.drive.user.domain.UserPrincipal
import org.springframework.security.access.PermissionEvaluator
import org.springframework.security.core.Authentication
import org.springframework.stereotype.Component
import java.io.Serializable

/**
 * `@PreAuthorize("hasPermission(#fileId, 'FILE', 'EDITOR')")` 형태로 사용.
 * 권한 규칙 변경 시 이 클래스와 PermissionService만 수정하면 된다.
 */
@Component
class DrivePermissionEvaluator(
    private val permissionService: PermissionService,
) : PermissionEvaluator {

    override fun hasPermission(
        authentication: Authentication,
        targetDomainObject: Any,
        permission: Any,
    ): Boolean {
        val fileId = targetDomainObject as? Long ?: return false
        val userId = (authentication.principal as? UserPrincipal)?.id ?: return false
        val required = runCatching { Permission.valueOf(permission.toString()) }.getOrNull() ?: return false
        val actual = permissionService.resolvePermission(fileId, userId)
        return actual != null && actual >= required
    }

    override fun hasPermission(
        authentication: Authentication,
        targetId: Serializable,
        targetType: String,
        permission: Any,
    ): Boolean {
        val fileId = targetId as? Long ?: return false
        val userId = (authentication.principal as? UserPrincipal)?.id ?: return false
        val required = runCatching { Permission.valueOf(permission.toString()) }.getOrNull() ?: return false
        val actual = permissionService.resolvePermission(fileId, userId)
        return actual != null && actual >= required
    }
}
