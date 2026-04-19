package demo.drive.trash.controller

import demo.drive.common.extension.currentUserId
import demo.drive.common.response.CommonResponse
import demo.drive.trash.controller.dto.TrashItemResponse
import demo.drive.trash.service.TrashService
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.*

@Controller
class TrashViewController(private val trashService: TrashService) {

    @GetMapping("/trash")
    fun trashPage(model: Model): String {
        model.addAttribute("files", trashService.listTrashed(currentUserId()).map { TrashItemResponse.from(it) })
        model.addAttribute("pageTitle", "휴지통")
        return "trash/index"
    }
}

@RestController
@RequestMapping("/api/trash")
class TrashController(private val trashService: TrashService) {

    @GetMapping
    fun list(): ResponseEntity<CommonResponse<List<TrashItemResponse>>> {
        val items = trashService.listTrashed(currentUserId()).map { TrashItemResponse.from(it) }
        return CommonResponse.ok(items)
    }

    @PostMapping("/{id}/restore")
    fun restore(@PathVariable id: Long): ResponseEntity<CommonResponse<Nothing>> {
        trashService.restore(id, currentUserId())
        return CommonResponse.noContent()
    }

    @DeleteMapping("/{id}")
    fun deletePermanently(@PathVariable id: Long): ResponseEntity<CommonResponse<Nothing>> {
        trashService.deletePermanently(id, currentUserId())
        return CommonResponse.noContent()
    }

    @DeleteMapping
    fun emptyTrash(): ResponseEntity<CommonResponse<Nothing>> {
        trashService.emptyTrash(currentUserId())
        return CommonResponse.noContent()
    }
}
