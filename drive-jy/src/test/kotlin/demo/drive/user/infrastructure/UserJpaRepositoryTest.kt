package demo.drive.user.infrastructure

import demo.drive.support.DataJpaTestBase
import demo.drive.user.domain.User
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class UserJpaRepositoryTest @Autowired constructor(
    private val userJpaRepository: UserJpaRepository,
) : DataJpaTestBase() {

    @Test
    fun `이메일로 사용자 조회`() {
        userJpaRepository.save(User(email = "a@b.com", password = "hashed", name = "이름"))

        val found = userJpaRepository.findByEmail("a@b.com")

        assertThat(found).isNotNull()
        assertThat(found!!.email).isEqualTo("a@b.com")
    }

    @Test
    fun `존재하지 않는 이메일 조회 시 null 반환`() {
        val found = userJpaRepository.findByEmail("none@b.com")
        assertThat(found).isNull()
    }

    @Test
    fun `이메일 중복 여부 확인`() {
        userJpaRepository.save(User(email = "dup@b.com", password = "hashed", name = "이름"))

        assertThat(userJpaRepository.existsByEmail("dup@b.com")).isTrue()
        assertThat(userJpaRepository.existsByEmail("new@b.com")).isFalse()
    }

    @Test
    fun `사용자 저장 후 ID 발급`() {
        val user = userJpaRepository.save(User(email = "new@b.com", password = "hashed", name = "이름"))
        assertThat(user.id).isGreaterThan(0)
    }
}
