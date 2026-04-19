# group 도메인

## 책임

그룹 생성·해산, 멤버 초대·추방, 그룹 내 역할(OWNER/MANAGER/MEMBER) 관리.

## 파일 구성

```
group/
├── controller/
│   ├── GroupController.kt      GET/POST /groups, /groups/{id}
│   │                           POST/PATCH/DELETE /api/groups/{id}/members/**
│   └── dto/
│       ├── CreateGroupRequest.kt
│       ├── InviteMemberRequest.kt
│       ├── UpdateRoleRequest.kt
│       └── GroupResponse.kt
├── service/
│   └── GroupService.kt
├── domain/
│   ├── Group.kt
│   ├── GroupMember.kt
│   ├── GroupRole.kt            enum: OWNER, MANAGER, MEMBER
│   ├── GroupRepository.kt      인터페이스
│   └── GroupMemberRepository.kt 인터페이스
└── infrastructure/
    ├── GroupJpaRepository.kt
    └── GroupMemberJpaRepository.kt
```

## 엔티티

```kotlin
@Entity @Table(name = "groups")
class Group(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    @Column(nullable = false)
    var name: String,
    var description: String? = null,
    @Column(name = "created_by", nullable = false)
    val createdBy: Long,            // 생성자 userId
)

@Entity @Table(name = "group_members")
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

enum class GroupRole { OWNER, MANAGER, MEMBER }
```

## GroupMemberRepository 주요 메서드

```kotlin
interface GroupMemberRepository {
    fun findByGroupId(groupId: Long): List<GroupMember>
    fun findByUserId(userId: Long): List<GroupMember>
    fun findGroupIdsByUserId(userId: Long): List<Long>   // permission 도메인에서 사용
    fun findByGroupIdAndUserId(groupId: Long, userId: Long): GroupMember?
    fun existsByGroupIdAndUserId(groupId: Long, userId: Long): Boolean
}
```

## GroupService 권한 규칙

| 작업 | 필요 역할 |
|------|-----------|
| 그룹 해산 | 그룹 OWNER |
| 멤버 초대 | OWNER 또는 MANAGER |
| 멤버 추방 | OWNER 또는 MANAGER (단, OWNER는 추방 불가) |
| 역할 변경 | OWNER만 가능 |
| 그룹 탈퇴 | OWNER는 탈퇴 불가 (소유권 이전 후 탈퇴) |
| 그룹 정보 수정 | OWNER 또는 MANAGER |

```kotlin
private fun requireGroupRole(groupId: Long, userId: Long, vararg roles: GroupRole) {
    val member = groupMemberRepository.findByGroupIdAndUserId(groupId, userId)
        ?: throw DriveException.accessDenied("그룹 접근")
    if (member.groupRole !in roles) throw DriveException.accessDenied("해당 작업")
}
```

## 규칙

- 그룹 생성 시 생성자는 자동으로 `GroupRole.OWNER` 부여.
- `GroupRole.OWNER`는 그룹당 반드시 1명 이상 유지. OWNER가 1명일 때 탈퇴/추방 시 에러.
- `findGroupIdsByUserId`는 `permission` 도메인의 `PermissionService`에서 그룹 권한 조회 시 호출됨 — 성능 주의, 캐싱 고려.
- 멤버 중복 초대 시 `DriveException.InvalidRequest("이미 그룹 멤버입니다.")` 반환.
