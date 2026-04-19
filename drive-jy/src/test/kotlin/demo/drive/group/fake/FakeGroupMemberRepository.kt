package demo.drive.group.fake

import demo.drive.group.domain.GroupMember
import demo.drive.group.domain.GroupMemberRepository
import java.lang.reflect.Field

class FakeGroupMemberRepository : GroupMemberRepository {
    private val store = mutableMapOf<Long, GroupMember>()
    private var sequence = 1L

    override fun findGroupIdsByUserId(userId: Long): List<Long> =
        store.values.filter { it.userId == userId }.map { it.groupId }

    override fun findByGroupId(groupId: Long): List<GroupMember> =
        store.values.filter { it.groupId == groupId }

    override fun findByGroupIdAndUserId(groupId: Long, userId: Long): GroupMember? =
        store.values.find { it.groupId == groupId && it.userId == userId }

    override fun existsByGroupIdAndUserId(groupId: Long, userId: Long): Boolean =
        store.values.any { it.groupId == groupId && it.userId == userId }

    override fun save(member: GroupMember): GroupMember {
        val id = if (member.id == 0L) sequence++ else member.id
        val saved = setId(member, id)
        store[id] = saved
        return saved
    }

    override fun delete(member: GroupMember) { store.remove(member.id) }

    override fun deleteAllByGroupId(groupId: Long) {
        store.entries.removeIf { it.value.groupId == groupId }
    }

    fun clear() { store.clear(); sequence = 1L }

    private fun setId(member: GroupMember, id: Long): GroupMember {
        val field: Field = GroupMember::class.java.getDeclaredField("id")
        field.isAccessible = true
        field.set(member, id)
        return member
    }
}
