package demo.drive.group.domain

interface GroupInvitationRepository {
    fun findById(id: Long): GroupInvitation?
    fun findByInviteeIdAndStatus(inviteeId: Long, status: InvitationStatus): List<GroupInvitation>
    fun findByGroupIdAndInviteeIdAndStatus(groupId: Long, inviteeId: Long, status: InvitationStatus): GroupInvitation?
    fun countByInviteeIdAndStatus(inviteeId: Long, status: InvitationStatus): Long
    fun save(invitation: GroupInvitation): GroupInvitation
}
