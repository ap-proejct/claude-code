package demo.drive.user.fake

import demo.drive.user.domain.SystemRole
import demo.drive.user.domain.User
import demo.drive.user.domain.UserRepository
import java.lang.reflect.Field

/**
 * UserRepository의 인메모리 Fake 구현체.
 * Service 단위 테스트에서 Spring 없이 사용한다.
 */
class FakeUserRepository : UserRepository {
    private val store = mutableMapOf<Long, User>()
    private var sequence = 1L

    override fun save(user: User): User {
        val id = if (user.id == 0L) sequence++ else user.id
        val saved = setId(user, id)
        store[id] = saved
        return saved
    }

    override fun findByEmail(email: String): User? =
        store.values.find { it.email == email }

    override fun findById(id: Long): User? = store[id]

    override fun existsByEmail(email: String): Boolean =
        store.values.any { it.email == email }

    fun clear() {
        store.clear()
        sequence = 1L
    }

    // val id 필드에 리플렉션으로 값 설정 (JPA와 동일한 방식)
    private fun setId(user: User, id: Long): User {
        val field: Field = User::class.java.getDeclaredField("id")
        field.isAccessible = true
        field.set(user, id)
        return user
    }
}
