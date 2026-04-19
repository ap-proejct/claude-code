package demo.drive.common.config

import demo.drive.common.security.DrivePermissionEvaluator
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler
import org.springframework.security.access.expression.method.MethodSecurityExpressionHandler
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity

/**
 * 메서드 단위 권한 제어.
 * 역할 추가·변경 시 SecurityConfig.roleHierarchy() 빈만 수정하면 전체 적용된다.
 * RoleHierarchy 빈은 Spring Security 7에서 자동으로 적용됨.
 *
 * 사용 예:
 *   @PreAuthorize("hasRole('ADMIN')")
 *   @PreAuthorize("hasPermission(#fileId, 'FILE', 'EDITOR')")
 */
@Configuration
@EnableMethodSecurity
class MethodSecurityConfig(
    private val permissionEvaluator: DrivePermissionEvaluator,
) {
    @Bean
    fun methodSecurityExpressionHandler(): MethodSecurityExpressionHandler =
        DefaultMethodSecurityExpressionHandler().apply {
            setPermissionEvaluator(permissionEvaluator)
        }
}
