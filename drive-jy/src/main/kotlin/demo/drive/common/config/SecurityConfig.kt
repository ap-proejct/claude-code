package demo.drive.common.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.access.hierarchicalroles.RoleHierarchy
import org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.SecurityFilterChain

@Configuration
@EnableWebSecurity
class SecurityConfig {

    @Bean
    fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder()

    /**
     * 역할 계층 정의. 새 역할 추가 시 여기만 수정.
     * ADMIN > USER — ADMIN은 USER의 모든 권한을 자동으로 포함한다.
     */
    @Bean
    fun roleHierarchy(): RoleHierarchy =
        RoleHierarchyImpl.fromHierarchy(
            """
            ROLE_ADMIN > ROLE_USER
            """.trimIndent()
        )

    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .authorizeHttpRequests {
                it.requestMatchers("/auth/**", "/share/**", "/api/share/resolve/**", "/css/**", "/js/**", "/icons/**").permitAll()
                it.requestMatchers("/h2-console/**").permitAll()
                it.anyRequest().authenticated()
            }
            .formLogin {
                it.loginPage("/auth/login")
                it.loginProcessingUrl("/auth/login")
                it.usernameParameter("email")
                it.passwordParameter("password")
                it.defaultSuccessUrl("/drive", true)
                it.failureUrl("/auth/login?error")
            }
            .logout {
                it.logoutUrl("/auth/logout")
                it.logoutSuccessUrl("/auth/login")
            }
            .rememberMe {
                it.key("drive-remember-me")
                it.tokenValiditySeconds(604800)
            }
            .csrf { it.disable() }
            .headers {
                it.frameOptions { fo -> fo.sameOrigin() }
            }
        return http.build()
    }
}
