package demo.drive.share.controller

import demo.drive.common.extension.currentUserId
import demo.drive.common.response.CommonResponse
import demo.drive.permission.domain.Permission
import demo.drive.share.controller.dto.ShareLinkRequest
import demo.drive.share.controller.dto.ShareLinkResponse
import demo.drive.share.controller.dto.SharedFileResponse
import demo.drive.share.service.ShareService
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.*

@Controller
class ShareViewController(private val shareService: ShareService) {

    @GetMapping("/share/{token}")
    fun sharedFilePage(@PathVariable token: String, model: Model): String {
        val (file, fp) = shareService.resolveSharedFile(token)
        model.addAttribute("file", SharedFileResponse.from(file, fp.permission))
        model.addAttribute("token", token)
        model.addAttribute("pageTitle", "${file.name} — 공유")
        return "share/view"
    }
}

@RestController
@RequestMapping("/api/share")
class ShareApiController(private val shareService: ShareService) {

    @PostMapping("/{fileId}")
    fun createLink(
        @PathVariable fileId: Long,
        @RequestBody req: ShareLinkRequest,
    ): ResponseEntity<CommonResponse<ShareLinkResponse>> {
        val permission = runCatching { Permission.valueOf(req.permission) }
            .getOrElse { Permission.VIEWER }
        val fp = shareService.createLink(fileId, currentUserId(), permission, req.expiresAt)
        return CommonResponse.created(ShareLinkResponse.from(fp))
    }

    @DeleteMapping("/{fileId}/{token}")
    fun revokeLink(
        @PathVariable fileId: Long,
        @PathVariable token: String,
    ): ResponseEntity<CommonResponse<Nothing>> {
        shareService.revokeLink(fileId, token, currentUserId())
        return CommonResponse.noContent()
    }

    @GetMapping("/{fileId}/links")
    fun listLinks(@PathVariable fileId: Long): ResponseEntity<CommonResponse<List<ShareLinkResponse>>> {
        val links = shareService.listLinks(fileId, currentUserId()).map { ShareLinkResponse.from(it) }
        return CommonResponse.ok(links)
    }

    @GetMapping("/resolve/{token}")
    fun resolveToken(@PathVariable token: String): ResponseEntity<CommonResponse<SharedFileResponse>> {
        val (file, fp) = shareService.resolveSharedFile(token)
        return CommonResponse.ok(SharedFileResponse.from(file, fp.permission))
    }
}
