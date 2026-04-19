package demo.drive.file.controller

import demo.drive.common.exception.DriveErrorCode
import demo.drive.common.exception.DriveException
import demo.drive.common.extension.currentUserId
import demo.drive.file.domain.FileType
import demo.drive.file.service.FileService
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable

@Controller
class DriveController(
    private val fileService: FileService,
) {

    @GetMapping("/drive")
    fun index(model: Model): String {
        val userId = currentUserId()
        val files = fileService.listFiles(parentId = null, userId = userId)
        model.addAttribute("files", files)
        model.addAttribute("currentPath", "/drive")
        model.addAttribute("parentId", null)
        model.addAttribute("breadcrumbs", emptyList<Any>())
        return "drive/index"
    }

    @GetMapping("/drive/shared-with-me")
    fun sharedWithMe(model: Model): String {
        val files = fileService.listSharedWithMe(currentUserId())
        model.addAttribute("files", files)
        model.addAttribute("currentPath", "/drive/shared-with-me")
        return "drive/shared-with-me"
    }

    @GetMapping("/drive/starred")
    fun starred(model: Model): String {
        val files = fileService.listStarred(currentUserId())
        model.addAttribute("files", files)
        model.addAttribute("currentPath", "/drive/starred")
        return "drive/starred"
    }

    @GetMapping("/drive/folder/{id}")
    fun folder(@PathVariable id: Long, model: Model): String {
        val userId = currentUserId()
        val folder = fileService.getFile(id, userId)
        if (folder.type != FileType.FOLDER) throw DriveException(DriveErrorCode.FILE_NOT_FOUND)
        val files = fileService.listFiles(parentId = id, userId = userId)
        model.addAttribute("folder", folder)
        model.addAttribute("files", files)
        model.addAttribute("currentPath", "/drive/folder/$id")
        model.addAttribute("parentId", id)
        return "drive/folder"
    }
}
