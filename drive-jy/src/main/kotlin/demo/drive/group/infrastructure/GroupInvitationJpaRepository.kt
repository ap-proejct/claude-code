package demo.drive.group.infrastructure

import demo.drive.group.domain.GroupInvitation
import demo.drive.group.domain.GroupInvitationRepository
import demo.drive.group.domain.InvitationStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

interface GroupInvitationSpringDataRepository : JpaRepository<GroupInvitation, Long> {
    fun findByInviteeIdAndStatus(inviteeId: Long, status: InvitationStatus): List<GroupInvitation>
    fun findByGroupIdAndInviteeIdAndStatus(groupId: Long, inviteeId: Long, status: InvitationStatus): GroupInvitation?
    fun countByInviteeIdAndStatus(inviteeId: Long, status: InvitationStatus): Long
}

@Repository
class GroupInvitationJpaRepository(
    private val jpa: GroupInvitationSpringDataRepository,
) : GroupInvitationRepository {
    override fun findById(id: Long) = jpa.findById(id).orElse(null)
    override fun findByInviteeIdAndStatus(inviteeId: Long, status: InvitationStatus) =
        jpa.findByInviteeIdAndStatus(inviteeId, status)
    override fun findByGroupIdAndInviteeIdAndStatus(groupId: Long, inviteeId: Long, status: InvitationStatus) =
        jpa.findByGroupIdAndInviteeIdAndStatus(groupId, inviteeId, status)
    override fun countByInviteeIdAndStatus(inviteeId: Long, status: InvitationStatus) =
        jpa.countByInviteeIdAndStatus(inviteeId, status)
    override fun save(invitation: GroupInvitation) = jpa.save(invitation)
}
