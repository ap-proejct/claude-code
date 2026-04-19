package demo.drive.group.domain

interface GroupMemberRepository {
    fun findGroupIdsByUserId(userId: Long): List<Long>
    fun findByGroupId(groupId: Long): List<GroupMember>
    fun findByGroupIdAndUserId(groupId: Long, userId: Long): GroupMember?
    fun existsByGroupIdAndUserId(groupId: Long, userId: Long): Boolean
    fun save(member: GroupMember): GroupMember
    fun delete(member: GroupMember)
    fun deleteAllByGroupId(groupId: Long)
}
