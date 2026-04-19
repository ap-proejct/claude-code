package demo.drive.user.infrastructure

import demo.drive.user.domain.User
import demo.drive.user.domain.UserRepository
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

interface UserSpringDataRepository : JpaRepository<User, Long> {
    fun findByEmail(email: String): User?
    fun existsByEmail(email: String): Boolean
}

@Repository
class UserJpaRepository(
    private val jpaRepo: UserSpringDataRepository,
) : UserRepository {
    override fun findByEmail(email: String): User? = jpaRepo.findByEmail(email)
    override fun findById(id: Long): User? = jpaRepo.findById(id).orElse(null)
    override fun save(user: User): User = jpaRepo.save(user)
    override fun existsByEmail(email: String): Boolean = jpaRepo.existsByEmail(email)
}
