package demo.drive.group.service

import demo.drive.common.exception.DriveErrorCode
import demo.drive.common.exception.DriveException
import demo.drive.group.domain.GroupRole
import demo.drive.group.fake.FakeGroupMemberRepository
import demo.drive.group.fake.FakeGroupRepository
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class GroupServiceTest {

    private val groupRepository = FakeGroupRepository()
    private val groupMemberRepository = FakeGroupMemberRepository()
    private val groupService = GroupService(groupRepository, groupMemberRepository)

    private val ownerId = 1L
    private val memberId = 2L
    private val outsiderId = 3L

    @BeforeEach
    fun setUp() {
        groupRepository.clear()
        groupMemberRepository.clear()
    }

    @Test
    fun `createGroup - 그룹 생성 시 생성자는 OWNER로 자동 등록`() {
        val group = groupService.createGroup("테스트그룹", null, ownerId)

        assertThat(group.name).isEqualTo("테스트그룹")
        val member = groupMemberRepository.findByGroupIdAndUserId(group.id, ownerId)
        assertThat(member).isNotNull()
        assertThat(member!!.groupRole).isEqualTo(GroupRole.OWNER)
    }

    @Test
    fun `listMyGroups - 내가 속한 그룹만 반환`() {
        val group1 = groupService.createGroup("그룹1", null, ownerId)
        groupService.createGroup("그룹2", null, memberId)

        val myGroups = groupService.listMyGroups(ownerId)
        assertThat(myGroups).hasSize(1)
        assertThat(myGroups[0].id).isEqualTo(group1.id)
    }

    @Test
    fun `inviteMember - OWNER는 멤버 초대 가능`() {
        val group = groupService.createGroup("그룹", null, ownerId)
        groupService.inviteMember(group.id, ownerId, memberId)

        assertThat(groupMemberRepository.existsByGroupIdAndUserId(group.id, memberId)).isTrue()
    }

    @Test
    fun `inviteMember - 중복 초대 시 INVALID_REQUEST`() {
        val group = groupService.createGroup("그룹", null, ownerId)
        groupService.inviteMember(group.id, ownerId, memberId)

        assertThatThrownBy { groupService.inviteMember(group.id, ownerId, memberId) }
            .isInstanceOf(DriveException::class.java)
            .hasFieldOrPropertyWithValue("errorCode", DriveErrorCode.INVALID_REQUEST)
    }

    @Test
    fun `inviteMember - 일반 멤버는 초대 불가 (ACCESS_DENIED)`() {
        val group = groupService.createGroup("그룹", null, ownerId)
        groupService.inviteMember(group.id, ownerId, memberId)

        assertThatThrownBy { groupService.inviteMember(group.id, memberId, outsiderId) }
            .isInstanceOf(DriveException::class.java)
            .hasFieldOrPropertyWithValue("errorCode", DriveErrorCode.ACCESS_DENIED)
    }

    @Test
    fun `removeMember - OWNER는 일반 멤버 추방 가능`() {
        val group = groupService.createGroup("그룹", null, ownerId)
        groupService.inviteMember(group.id, ownerId, memberId)

        groupService.removeMember(group.id, ownerId, memberId)

        assertThat(groupMemberRepository.existsByGroupIdAndUserId(group.id, memberId)).isFalse()
    }

    @Test
    fun `removeMember - OWNER 추방 시 GROUP_MUST_HAVE_OWNER`() {
        val group = groupService.createGroup("그룹", null, ownerId)

        assertThatThrownBy { groupService.removeMember(group.id, ownerId, ownerId) }
            .isInstanceOf(DriveException::class.java)
            .hasFieldOrPropertyWithValue("errorCode", DriveErrorCode.GROUP_MUST_HAVE_OWNER)
    }

    @Test
    fun `leave - 일반 멤버는 탈퇴 가능`() {
        val group = groupService.createGroup("그룹", null, ownerId)
        groupService.inviteMember(group.id, ownerId, memberId)

        groupService.leave(group.id, memberId)

        assertThat(groupMemberRepository.existsByGroupIdAndUserId(group.id, memberId)).isFalse()
    }

    @Test
    fun `leave - OWNER는 탈퇴 불가`() {
        val group = groupService.createGroup("그룹", null, ownerId)

        assertThatThrownBy { groupService.leave(group.id, ownerId) }
            .isInstanceOf(DriveException::class.java)
            .hasFieldOrPropertyWithValue("errorCode", DriveErrorCode.GROUP_MUST_HAVE_OWNER)
    }

    @Test
    fun `changeRole - OWNER는 역할 변경 가능`() {
        val group = groupService.createGroup("그룹", null, ownerId)
        groupService.inviteMember(group.id, ownerId, memberId)

        groupService.changeRole(group.id, ownerId, memberId, GroupRole.MANAGER)

        val member = groupMemberRepository.findByGroupIdAndUserId(group.id, memberId)
        assertThat(member!!.groupRole).isEqualTo(GroupRole.MANAGER)
    }

    @Test
    fun `dissolve - OWNER는 그룹 해산 가능`() {
        val group = groupService.createGroup("그룹", null, ownerId)
        groupService.inviteMember(group.id, ownerId, memberId)

        groupService.dissolve(group.id, ownerId)

        assertThat(groupRepository.findById(group.id)).isNull()
        assertThat(groupMemberRepository.findByGroupId(group.id)).isEmpty()
    }
}
