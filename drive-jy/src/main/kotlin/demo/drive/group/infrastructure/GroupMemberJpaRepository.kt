package demo.drive.group.infrastructure

import demo.drive.group.domain.GroupMember
import demo.drive.group.domain.GroupMemberRepository
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

interface GroupMemberSpringDataRepository : JpaRepository<GroupMember, Long> {
    fun findByGroupId(groupId: Long): List<GroupMember>
    fun findByGroupIdAndUserId(groupId: Long, userId: Long): GroupMember?
    fun existsByGroupIdAndUserId(groupId: Long, userId: Long): Boolean
    fun deleteAllByGroupId(groupId: Long)

    @Query("SELECT m.groupId FROM GroupMember m WHERE m.userId = :userId")
    fun findGroupIdsByUserId(@Param("userId") userId: Long): List<Long>
}

@Repository
class GroupMemberJpaRepository(
    private val jpaRepo: GroupMemberSpringDataRepository,
) : GroupMemberRepository {
    override fun findGroupIdsByUserId(userId: Long): List<Long> =
        jpaRepo.findGroupIdsByUserId(userId)
    override fun findByGroupId(groupId: Long): List<GroupMember> =
        jpaRepo.findByGroupId(groupId)
    override fun findByGroupIdAndUserId(groupId: Long, userId: Long): GroupMember? =
        jpaRepo.findByGroupIdAndUserId(groupId, userId)
    override fun existsByGroupIdAndUserId(groupId: Long, userId: Long): Boolean =
        jpaRepo.existsByGroupIdAndUserId(groupId, userId)
    override fun save(member: GroupMember): GroupMember = jpaRepo.save(member)
    override fun delete(member: GroupMember) = jpaRepo.delete(member)
    override fun deleteAllByGroupId(groupId: Long) = jpaRepo.deleteAllByGroupId(groupId)
}
