package demo.drive.group.controller.dto

import demo.drive.group.domain.GroupInvitation
import java.time.ZoneId
import java.time.format.DateTimeFormatter

data class GroupInvitationResponse(
    val id: Long,
    val groupId: Long,
    val groupName: String,
    val inviterId: Long,
    val inviterName: String,
    val inviteeId: Long,
    val status: String,
    val createdAt: String,
) {
    companion object {
        private val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
            .withZone(ZoneId.of("Asia/Seoul"))

        fun from(invitation: GroupInvitation, groupName: String, inviterName: String) =
            GroupInvitationResponse(
                id = invitation.id,
                groupId = invitation.groupId,
                groupName = groupName,
                inviterId = invitation.inviterId,
                inviterName = inviterName,
                inviteeId = invitation.inviteeId,
                status = invitation.status.name,
                createdAt = formatter.format(invitation.createdAt),
            )
    }
}
