package demo.drive.group.controller

import demo.drive.common.extension.currentUserId
import demo.drive.common.response.CommonResponse
import demo.drive.group.controller.dto.CreateGroupRequest
import demo.drive.group.controller.dto.GroupMemberResponse
import demo.drive.group.controller.dto.GroupResponse
import demo.drive.group.controller.dto.InviteMemberRequest
import demo.drive.group.controller.dto.UpdateRoleRequest
import demo.drive.group.domain.GroupRole
import demo.drive.group.service.GroupService
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.*

@Controller
class GroupViewController(private val groupService: GroupService) {

    @GetMapping("/groups")
    fun groupListPage(model: Model): String {
        val groups = groupService.listMyGroups(currentUserId()).map { GroupResponse.from(it) }
        model.addAttribute("groups", groups)
        model.addAttribute("pageTitle", "그룹")
        return "groups/list"
    }

    @GetMapping("/groups/{id}")
    fun groupDetailPage(@PathVariable id: Long, model: Model): String {
        val userId = currentUserId()
        val group = groupService.getGroup(id, userId)
        val members = groupService.listMembers(id, userId).map { GroupMemberResponse.from(it) }
        val myRole = members.find { it.userId == userId }?.groupRole ?: "MEMBER"
        model.addAttribute("group", GroupResponse.from(group))
        model.addAttribute("members", members)
        model.addAttribute("myRole", myRole)
        model.addAttribute("currentUserId", userId)
        model.addAttribute("pageTitle", group.name)
        return "groups/detail"
    }
}

@RestController
@RequestMapping("/api/groups")
class GroupApiController(private val groupService: GroupService) {

    @PostMapping
    fun createGroup(@RequestBody req: CreateGroupRequest): ResponseEntity<CommonResponse<GroupResponse>> {
        val group = groupService.createGroup(req.name, req.description, currentUserId())
        return CommonResponse.created(GroupResponse.from(group))
    }

    @DeleteMapping("/{id}")
    fun dissolve(@PathVariable id: Long): ResponseEntity<CommonResponse<Nothing>> {
        groupService.dissolve(id, currentUserId())
        return CommonResponse.noContent()
    }

    @GetMapping("/{id}/members")
    fun listMembers(@PathVariable id: Long): ResponseEntity<CommonResponse<List<GroupMemberResponse>>> {
        val members = groupService.listMembers(id, currentUserId()).map { GroupMemberResponse.from(it) }
        return CommonResponse.ok(members)
    }

    @PostMapping("/{id}/members")
    fun inviteMember(
        @PathVariable id: Long,
        @RequestBody req: InviteMemberRequest,
    ): ResponseEntity<CommonResponse<Nothing>> {
        groupService.inviteMember(id, currentUserId(), req.userId)
        return CommonResponse.noContent()
    }

    @DeleteMapping("/{id}/members/{userId}")
    fun removeMember(
        @PathVariable id: Long,
        @PathVariable userId: Long,
    ): ResponseEntity<CommonResponse<Nothing>> {
        val currentId = currentUserId()
        if (currentId == userId) {
            groupService.leave(id, currentId)
        } else {
            groupService.removeMember(id, currentId, userId)
        }
        return CommonResponse.noContent()
    }

    @PatchMapping("/{id}/members/{userId}/role")
    fun changeRole(
        @PathVariable id: Long,
        @PathVariable userId: Long,
        @RequestBody req: UpdateRoleRequest,
    ): ResponseEntity<CommonResponse<Nothing>> {
        val newRole = runCatching { GroupRole.valueOf(req.role) }
            .getOrElse { throw demo.drive.common.exception.DriveException.invalidRequest("유효하지 않은 역할입니다.") }
        groupService.changeRole(id, currentUserId(), userId, newRole)
        return CommonResponse.noContent()
    }
}
