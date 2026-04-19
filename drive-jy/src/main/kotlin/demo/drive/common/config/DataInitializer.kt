package demo.drive.common.config

import demo.drive.user.domain.SystemRole
import demo.drive.user.domain.User
import demo.drive.user.domain.UserRepository
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.context.annotation.Profile
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component

@Component
@Profile("dev")
class DataInitializer(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
) : ApplicationRunner {

    override fun run(args: ApplicationArguments) {
        if (!userRepository.existsByEmail("admin@drive.com")) {
            userRepository.save(
                User(
                    email = "admin@drive.com",
                    password = passwordEncoder.encode("admin1234")
                        ?: throw IllegalStateException("비밀번호 인코딩 실패"),
                    name = "관리자",
                    systemRole = SystemRole.ADMIN,
                )
            )
        }
    }
}
