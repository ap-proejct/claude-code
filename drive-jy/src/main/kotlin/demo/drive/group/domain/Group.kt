package demo.drive.group.domain

import jakarta.persistence.*

@Entity
@Table(name = "user_groups")
class Group(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false)
    var name: String,

    var description: String? = null,

    @Column(name = "created_by", nullable = false)
    val createdBy: Long,
)
