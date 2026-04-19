package demo.drive.group.infrastructure

import demo.drive.group.domain.Group
import demo.drive.group.domain.GroupRepository
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

interface GroupSpringDataRepository : JpaRepository<Group, Long> {
    fun findByIdIn(ids: List<Long>): List<Group>
}

@Repository
class GroupJpaRepository(
    private val jpaRepo: GroupSpringDataRepository,
) : GroupRepository {
    override fun findById(id: Long): Group? = jpaRepo.findById(id).orElse(null)
    override fun findByIdIn(ids: List<Long>): List<Group> =
        if (ids.isEmpty()) emptyList() else jpaRepo.findByIdIn(ids)
    override fun save(group: Group): Group = jpaRepo.save(group)
    override fun delete(group: Group) = jpaRepo.delete(group)
}
