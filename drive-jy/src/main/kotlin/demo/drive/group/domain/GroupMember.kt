package demo.drive.group.domain

import jakarta.persistence.*

@Entity
@Table(name = "group_members")
class GroupMember(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(name = "group_id", nullable = false)
    val groupId: Long,

    @Column(name = "user_id", nullable = false)
    val userId: Long,

    @Enumerated(EnumType.STRING)
    @Column(name = "group_role", nullable = false)
    var groupRole: GroupRole = GroupRole.MEMBER,
)
