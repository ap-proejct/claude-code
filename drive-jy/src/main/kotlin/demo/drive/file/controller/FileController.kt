package demo.drive.file.controller

import demo.drive.common.extension.currentUserId
import demo.drive.common.response.CommonResponse
import demo.drive.file.controller.dto.CreateFolderRequest
import demo.drive.file.controller.dto.FileResponse
import demo.drive.file.controller.dto.MoveRequest
import demo.drive.file.controller.dto.RenameRequest
import demo.drive.file.service.FileService
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

@RestController
@RequestMapping("/api/files")
class FileController(
    private val fileService: FileService,
) {

    @PostMapping("/upload")
    fun upload(
        @RequestParam("file") file: MultipartFile,
        @RequestParam("parentId", required = false) parentId: Long?,
    ): ResponseEntity<CommonResponse<FileResponse>> {
        val uploaded = fileService.upload(file, parentId, currentUserId())
        return CommonResponse.created(FileResponse.from(uploaded))
    }

    @PostMapping("/folders")
    fun createFolder(
        @RequestBody req: CreateFolderRequest,
    ): ResponseEntity<CommonResponse<FileResponse>> {
        val folder = fileService.createFolder(req.name, req.parentId, currentUserId())
        return CommonResponse.created(FileResponse.from(folder))
    }

    @GetMapping("/{id}")
    fun getFile(@PathVariable id: Long): ResponseEntity<CommonResponse<FileResponse>> {
        val file = fileService.getFile(id, currentUserId())
        return CommonResponse.ok(FileResponse.from(file))
    }

    @GetMapping("/{id}/download")
    fun download(@PathVariable id: Long, response: HttpServletResponse) {
        val (file, resource) = fileService.download(id, currentUserId())
        val encodedName = URLEncoder.encode(file.name, StandardCharsets.UTF_8).replace("+", "%20")
        val safeName = file.name.replace("\"", "\\\"")
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION,
            "attachment; filename=\"$safeName\"; filename*=UTF-8''$encodedName")
        response.contentType = "application/octet-stream"
        response.setContentLengthLong(file.sizeBytes)
        resource.inputStream.use { it.copyTo(response.outputStream) }
    }

    @PatchMapping("/{id}/rename")
    fun rename(
        @PathVariable id: Long,
        @RequestBody req: RenameRequest,
    ): ResponseEntity<CommonResponse<FileResponse>> {
        val file = fileService.rename(id, req.name, currentUserId())
        return CommonResponse.ok(FileResponse.from(file))
    }

    @PatchMapping("/{id}/move")
    fun move(
        @PathVariable id: Long,
        @RequestBody req: MoveRequest,
    ): ResponseEntity<CommonResponse<FileResponse>> {
        val file = fileService.move(id, req.parentId, currentUserId())
        return CommonResponse.ok(FileResponse.from(file))
    }

    @PatchMapping("/{id}/star")
    fun toggleStar(@PathVariable id: Long): ResponseEntity<CommonResponse<FileResponse>> {
        val file = fileService.toggleStar(id, currentUserId())
        return CommonResponse.ok(FileResponse.from(file))
    }

    @GetMapping("/{id}/preview")
    fun preview(@PathVariable id: Long, response: HttpServletResponse) {
        val (file, resource) = fileService.download(id, currentUserId())
        val encodedName = URLEncoder.encode(file.name, StandardCharsets.UTF_8).replace("+", "%20")
        val safeName = file.name.replace("\"", "\\\"")
        val previewable = file.mimeType?.let {
            it.startsWith("image/") || it == "application/pdf" || it.startsWith("text/")
        } ?: false
        if (previewable) {
            response.contentType = file.mimeType!!
            response.setHeader(HttpHeaders.CONTENT_DISPOSITION,
                "inline; filename=\"$safeName\"; filename*=UTF-8''$encodedName")
        } else {
            response.contentType = "application/octet-stream"
            response.setHeader(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"$safeName\"; filename*=UTF-8''$encodedName")
        }
        response.setContentLengthLong(file.sizeBytes)
        resource.inputStream.use { it.copyTo(response.outputStream) }
    }

    @DeleteMapping("/{id}")
    fun moveToTrash(@PathVariable id: Long): ResponseEntity<CommonResponse<Nothing>> {
        fileService.moveToTrash(id, currentUserId())
        return CommonResponse.noContent()
    }
}
