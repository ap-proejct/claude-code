package demo.drive.group.controller.dto

import demo.drive.group.domain.Group
import demo.drive.group.domain.GroupMember

data class GroupResponse(
    val id: Long,
    val name: String,
    val description: String?,
    val createdBy: Long,
) {
    companion object {
        fun from(group: Group) = GroupResponse(
            id = group.id,
            name = group.name,
            description = group.description,
            createdBy = group.createdBy,
        )
    }
}

data class GroupMemberResponse(
    val userId: Long,
    val groupRole: String,
) {
    companion object {
        fun from(member: GroupMember) = GroupMemberResponse(
            userId = member.userId,
            groupRole = member.groupRole.name,
        )
    }
}
