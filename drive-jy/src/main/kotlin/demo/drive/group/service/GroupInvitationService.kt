package demo.drive.group.service

import demo.drive.common.exception.DriveErrorCode
import demo.drive.common.exception.DriveException
import demo.drive.group.controller.dto.GroupInvitationResponse
import demo.drive.group.domain.GroupInvitation
import demo.drive.group.domain.GroupInvitationRepository
import demo.drive.group.domain.GroupMember
import demo.drive.group.domain.GroupMemberRepository
import demo.drive.group.domain.GroupRepository
import demo.drive.group.domain.GroupRole
import demo.drive.group.domain.InvitationStatus
import demo.drive.user.domain.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class GroupInvitationService(
    private val invitationRepository: GroupInvitationRepository,
    private val groupRepository: GroupRepository,
    private val groupMemberRepository: GroupMemberRepository,
    private val userRepository: UserRepository,
) {
    fun createInvitation(groupId: Long, inviterId: Long, inviteeId: Long): GroupInvitation {
        val inviterMember = groupMemberRepository.findByGroupIdAndUserId(groupId, inviterId)
            ?: throw DriveException(DriveErrorCode.ACCESS_DENIED)
        if (inviterMember.groupRole !in listOf(GroupRole.OWNER, GroupRole.MANAGER)) {
            throw DriveException(DriveErrorCode.ACCESS_DENIED)
        }
        if (groupMemberRepository.existsByGroupIdAndUserId(groupId, inviteeId)) {
            throw DriveException.invalidRequest("이미 그룹 멤버입니다.")
        }
        if (invitationRepository.findByGroupIdAndInviteeIdAndStatus(groupId, inviteeId, InvitationStatus.PENDING) != null) {
            throw DriveException.invalidRequest("이미 대기 중인 초대가 있습니다.")
        }
        userRepository.findById(inviteeId) ?: throw DriveException(DriveErrorCode.USER_NOT_FOUND)
        return invitationRepository.save(GroupInvitation(groupId = groupId, inviterId = inviterId, inviteeId = inviteeId))
    }

    @Transactional(readOnly = true)
    fun listPending(userId: Long): List<GroupInvitationResponse> {
        return invitationRepository.findByInviteeIdAndStatus(userId, InvitationStatus.PENDING).map { inv ->
            val group = groupRepository.findById(inv.groupId)
            val inviter = userRepository.findById(inv.inviterId)
            GroupInvitationResponse.from(
                inv,
                groupName = group?.name ?: "알 수 없는 그룹",
                inviterName = inviter?.name ?: "알 수 없는 사용자",
            )
        }
    }

    @Transactional(readOnly = true)
    fun countPending(userId: Long): Long =
        invitationRepository.countByInviteeIdAndStatus(userId, InvitationStatus.PENDING)

    fun accept(invitationId: Long, userId: Long) {
        val invitation = invitationRepository.findById(invitationId)
            ?: throw DriveException(DriveErrorCode.INVITATION_NOT_FOUND)
        if (invitation.inviteeId != userId) throw DriveException(DriveErrorCode.ACCESS_DENIED)
        if (invitation.status != InvitationStatus.PENDING) {
            throw DriveException(DriveErrorCode.INVITATION_ALREADY_PROCESSED)
        }
        invitation.status = InvitationStatus.ACCEPTED
        invitationRepository.save(invitation)
        if (!groupMemberRepository.existsByGroupIdAndUserId(invitation.groupId, userId)) {
            groupMemberRepository.save(GroupMember(groupId = invitation.groupId, userId = userId))
        }
    }

    fun reject(invitationId: Long, userId: Long) {
        val invitation = invitationRepository.findById(invitationId)
            ?: throw DriveException(DriveErrorCode.INVITATION_NOT_FOUND)
        if (invitation.inviteeId != userId) throw DriveException(DriveErrorCode.ACCESS_DENIED)
        if (invitation.status != InvitationStatus.PENDING) {
            throw DriveException(DriveErrorCode.INVITATION_ALREADY_PROCESSED)
        }
        invitation.status = InvitationStatus.REJECTED
        invitationRepository.save(invitation)
    }
}
