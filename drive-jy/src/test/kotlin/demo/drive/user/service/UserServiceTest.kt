package demo.drive.user.service

import demo.drive.common.exception.DriveErrorCode
import demo.drive.common.exception.DriveException
import demo.drive.user.fake.FakeUserRepository
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import kotlin.test.Test

/**
 * Spring 컨텍스트 없이 순수 Kotlin으로 실행.
 * 의존성은 FakeUserRepository + 실제 BCryptPasswordEncoder.
 */
class UserServiceTest {

    private lateinit var userRepository: FakeUserRepository
    private lateinit var userService: UserService

    @BeforeEach
    fun setUp() {
        userRepository = FakeUserRepository()
        userService = UserService(userRepository, BCryptPasswordEncoder())
    }

    @Test
    fun `회원가입 성공`() {
        val user = userService.register("test@example.com", "password123", "테스트유저")

        assertThat(user.id).isGreaterThan(0)
        assertThat(user.email).isEqualTo("test@example.com")
        assertThat(user.name).isEqualTo("테스트유저")
        assertThat(user.password).doesNotContain("password123")
    }

    @Test
    fun `중복 이메일 회원가입 실패`() {
        userService.register("dup@example.com", "password123", "첫번째")

        assertThatThrownBy { userService.register("dup@example.com", "password456", "두번째") }
            .isInstanceOf(DriveException::class.java)
            .hasFieldOrPropertyWithValue("errorCode", DriveErrorCode.USER_EMAIL_DUPLICATE)
    }

    @Test
    fun `이메일로 사용자 조회 성공`() {
        userService.register("find@example.com", "password123", "찾기유저")

        val found = userService.findByEmail("find@example.com")
        assertThat(found.email).isEqualTo("find@example.com")
    }

    @Test
    fun `존재하지 않는 이메일 조회 시 예외`() {
        assertThatThrownBy { userService.findByEmail("notexist@example.com") }
            .isInstanceOf(DriveException::class.java)
            .hasFieldOrPropertyWithValue("errorCode", DriveErrorCode.USER_NOT_FOUND)
    }

    @Test
    fun `스토리지 사용량 추가`() {
        val user = userService.register("storage@example.com", "password123", "유저")
        userService.addStorageUsage(user.id, 1024L)

        val updated = userRepository.findById(user.id)!!
        assertThat(updated.storageUsedBytes).isEqualTo(1024L)
    }

    @Test
    fun `스토리지 초과 시 예외`() {
        val user = userService.register("full@example.com", "password123", "유저")
        userService.addStorageUsage(user.id, user.storageLimitBytes)

        assertThatThrownBy { userService.checkStorageAvailable(user.id, 1L) }
            .isInstanceOf(DriveException::class.java)
            .hasFieldOrPropertyWithValue("errorCode", DriveErrorCode.STORAGE_LIMIT_EXCEEDED)
    }
}
