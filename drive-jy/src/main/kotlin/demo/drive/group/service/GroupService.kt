package demo.drive.group.service

import demo.drive.common.exception.DriveErrorCode
import demo.drive.common.exception.DriveException
import demo.drive.group.domain.Group
import demo.drive.group.domain.GroupMember
import demo.drive.group.domain.GroupMemberRepository
import demo.drive.group.domain.GroupRepository
import demo.drive.group.domain.GroupRole
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class GroupService(
    private val groupRepository: GroupRepository,
    private val groupMemberRepository: GroupMemberRepository,
) {
    fun createGroup(name: String, description: String?, createdBy: Long): Group {
        val group = groupRepository.save(Group(name = name, description = description, createdBy = createdBy))
        groupMemberRepository.save(GroupMember(groupId = group.id, userId = createdBy, groupRole = GroupRole.OWNER))
        return group
    }

    @Transactional(readOnly = true)
    fun listMyGroups(userId: Long): List<Group> {
        val groupIds = groupMemberRepository.findGroupIdsByUserId(userId)
        return groupRepository.findByIdIn(groupIds)
    }

    @Transactional(readOnly = true)
    fun getGroup(groupId: Long, userId: Long): Group {
        requireMember(groupId, userId)
        return groupRepository.findById(groupId) ?: throw DriveException(DriveErrorCode.GROUP_NOT_FOUND)
    }

    @Transactional(readOnly = true)
    fun listMembers(groupId: Long, userId: Long): List<GroupMember> {
        requireMember(groupId, userId)
        return groupMemberRepository.findByGroupId(groupId)
    }

    fun dissolve(groupId: Long, userId: Long) {
        requireGroupRole(groupId, userId, GroupRole.OWNER)
        groupMemberRepository.deleteAllByGroupId(groupId)
        val group = groupRepository.findById(groupId) ?: throw DriveException(DriveErrorCode.GROUP_NOT_FOUND)
        groupRepository.delete(group)
    }

    fun inviteMember(groupId: Long, inviterId: Long, targetUserId: Long): GroupMember {
        requireGroupRole(groupId, inviterId, GroupRole.OWNER, GroupRole.MANAGER)
        if (groupMemberRepository.existsByGroupIdAndUserId(groupId, targetUserId)) {
            throw DriveException.invalidRequest("이미 그룹 멤버입니다.")
        }
        return groupMemberRepository.save(GroupMember(groupId = groupId, userId = targetUserId))
    }

    fun removeMember(groupId: Long, requesterId: Long, targetUserId: Long) {
        requireGroupRole(groupId, requesterId, GroupRole.OWNER, GroupRole.MANAGER)
        val target = groupMemberRepository.findByGroupIdAndUserId(groupId, targetUserId)
            ?: throw DriveException(DriveErrorCode.GROUP_MEMBER_NOT_FOUND)
        if (target.groupRole == GroupRole.OWNER) {
            throw DriveException(DriveErrorCode.GROUP_MUST_HAVE_OWNER)
        }
        groupMemberRepository.delete(target)
    }

    fun leave(groupId: Long, userId: Long) {
        val member = groupMemberRepository.findByGroupIdAndUserId(groupId, userId)
            ?: throw DriveException(DriveErrorCode.GROUP_MEMBER_NOT_FOUND)
        if (member.groupRole == GroupRole.OWNER) {
            throw DriveException(DriveErrorCode.GROUP_MUST_HAVE_OWNER)
        }
        groupMemberRepository.delete(member)
    }

    fun changeRole(groupId: Long, requesterId: Long, targetUserId: Long, newRole: GroupRole) {
        requireGroupRole(groupId, requesterId, GroupRole.OWNER)
        val target = groupMemberRepository.findByGroupIdAndUserId(groupId, targetUserId)
            ?: throw DriveException(DriveErrorCode.GROUP_MEMBER_NOT_FOUND)
        target.groupRole = newRole
        groupMemberRepository.save(target)
    }

    private fun requireMember(groupId: Long, userId: Long) {
        if (!groupMemberRepository.existsByGroupIdAndUserId(groupId, userId)) {
            throw DriveException(DriveErrorCode.ACCESS_DENIED)
        }
    }

    private fun requireGroupRole(groupId: Long, userId: Long, vararg roles: GroupRole) {
        val member = groupMemberRepository.findByGroupIdAndUserId(groupId, userId)
            ?: throw DriveException(DriveErrorCode.ACCESS_DENIED)
        if (member.groupRole !in roles) throw DriveException(DriveErrorCode.ACCESS_DENIED)
    }
}
