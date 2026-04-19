package demo.drive.group.controller

import demo.drive.common.extension.currentUserId
import demo.drive.common.response.CommonResponse
import demo.drive.group.controller.dto.GroupInvitationResponse
import demo.drive.group.controller.dto.InviteMemberRequest
import demo.drive.group.service.GroupInvitationService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
class GroupInvitationController(private val groupInvitationService: GroupInvitationService) {

    @PostMapping("/api/groups/{id}/invitations")
    fun invite(
        @PathVariable id: Long,
        @RequestBody req: InviteMemberRequest,
    ): ResponseEntity<CommonResponse<Nothing>> {
        groupInvitationService.createInvitation(id, currentUserId(), req.userId)
        return CommonResponse.noContent()
    }

    @GetMapping("/api/invitations")
    fun listPending(): ResponseEntity<CommonResponse<List<GroupInvitationResponse>>> {
        val list = groupInvitationService.listPending(currentUserId())
        return CommonResponse.ok(list)
    }

    @PostMapping("/api/invitations/{id}/accept")
    fun accept(@PathVariable id: Long): ResponseEntity<CommonResponse<Nothing>> {
        groupInvitationService.accept(id, currentUserId())
        return CommonResponse.noContent()
    }

    @PostMapping("/api/invitations/{id}/reject")
    fun reject(@PathVariable id: Long): ResponseEntity<CommonResponse<Nothing>> {
        groupInvitationService.reject(id, currentUserId())
        return CommonResponse.noContent()
    }
}
