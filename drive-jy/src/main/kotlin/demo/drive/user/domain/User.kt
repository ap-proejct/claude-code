package demo.drive.user.domain

import jakarta.persistence.*

@Entity
@Table(name = "users")
class User(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(unique = true, nullable = false)
    var email: String,

    @Column(nullable = false)
    var password: String,

    @Column(nullable = false)
    var name: String,

    @Enumerated(EnumType.STRING)
    @Column(name = "system_role", nullable = false)
    var systemRole: SystemRole = SystemRole.USER,

    @Column(name = "storage_limit_bytes", nullable = false)
    var storageLimitBytes: Long = 16_106_127_360L,

    @Column(name = "storage_used_bytes", nullable = false)
    var storageUsedBytes: Long = 0L,
)
