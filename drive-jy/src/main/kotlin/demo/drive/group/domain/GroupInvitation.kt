package demo.drive.group.domain

import jakarta.persistence.*
import java.time.Instant

@Entity
@Table(name = "group_invitations")
class GroupInvitation(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(name = "group_id", nullable = false)
    val groupId: Long,

    @Column(name = "inviter_id", nullable = false)
    val inviterId: Long,

    @Column(name = "invitee_id", nullable = false)
    val inviteeId: Long,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: InvitationStatus = InvitationStatus.PENDING,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now(),
)
