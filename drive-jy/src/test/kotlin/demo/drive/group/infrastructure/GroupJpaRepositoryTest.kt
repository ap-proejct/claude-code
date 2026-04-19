package demo.drive.group.infrastructure

import demo.drive.group.domain.Group
import demo.drive.group.domain.GroupMember
import demo.drive.group.domain.GroupRole
import demo.drive.support.DataJpaTestBase
import demo.drive.user.domain.User
import demo.drive.user.infrastructure.UserJpaRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class GroupJpaRepositoryTest @Autowired constructor(
    private val groupJpaRepository: GroupJpaRepository,
    private val groupMemberJpaRepository: GroupMemberJpaRepository,
    private val userJpaRepository: UserJpaRepository,
) : DataJpaTestBase() {

    private fun createUser(email: String = "owner@test.com"): User =
        userJpaRepository.save(User(email = email, password = "hash", name = "테스터"))

    private fun createGroup(ownerId: Long, name: String = "테스트그룹"): Group =
        groupJpaRepository.save(Group(name = name, createdBy = ownerId))

    @Test
    fun `그룹 저장 및 ID 발급`() {
        val user = createUser()
        val group = createGroup(user.id)
        assertThat(group.id).isGreaterThan(0)
    }

    @Test
    fun `findByIdIn - 여러 그룹 조회`() {
        val user = createUser()
        val g1 = createGroup(user.id, "그룹1")
        val g2 = createGroup(user.id, "그룹2")
        createGroup(user.id, "그룹3")

        val found = groupJpaRepository.findByIdIn(listOf(g1.id, g2.id))
        assertThat(found).hasSize(2)
    }

    @Test
    fun `GroupMember 저장 및 조회`() {
        val user = createUser()
        val group = createGroup(user.id)
        groupMemberJpaRepository.save(GroupMember(groupId = group.id, userId = user.id, groupRole = GroupRole.OWNER))

        val members = groupMemberJpaRepository.findByGroupId(group.id)
        assertThat(members).hasSize(1)
        assertThat(members[0].groupRole).isEqualTo(GroupRole.OWNER)
    }

    @Test
    fun `findGroupIdsByUserId - 사용자가 속한 그룹 ID 반환`() {
        val user = createUser()
        val g1 = createGroup(user.id, "그룹A")
        val g2 = createGroup(user.id, "그룹B")
        groupMemberJpaRepository.save(GroupMember(groupId = g1.id, userId = user.id, groupRole = GroupRole.OWNER))
        groupMemberJpaRepository.save(GroupMember(groupId = g2.id, userId = user.id, groupRole = GroupRole.MEMBER))

        val groupIds = groupMemberJpaRepository.findGroupIdsByUserId(user.id)
        assertThat(groupIds).containsExactlyInAnyOrder(g1.id, g2.id)
    }

    @Test
    fun `deleteAllByGroupId - 그룹 멤버 전체 삭제`() {
        val user1 = createUser("u1@test.com")
        val user2 = createUser("u2@test.com")
        val group = createGroup(user1.id)
        groupMemberJpaRepository.save(GroupMember(groupId = group.id, userId = user1.id, groupRole = GroupRole.OWNER))
        groupMemberJpaRepository.save(GroupMember(groupId = group.id, userId = user2.id, groupRole = GroupRole.MEMBER))

        groupMemberJpaRepository.deleteAllByGroupId(group.id)

        assertThat(groupMemberJpaRepository.findByGroupId(group.id)).isEmpty()
    }

    @Test
    fun `existsByGroupIdAndUserId - 멤버 존재 여부 확인`() {
        val user = createUser()
        val group = createGroup(user.id)
        groupMemberJpaRepository.save(GroupMember(groupId = group.id, userId = user.id, groupRole = GroupRole.OWNER))

        assertThat(groupMemberJpaRepository.existsByGroupIdAndUserId(group.id, user.id)).isTrue()
        assertThat(groupMemberJpaRepository.existsByGroupIdAndUserId(group.id, 999L)).isFalse()
    }
}
