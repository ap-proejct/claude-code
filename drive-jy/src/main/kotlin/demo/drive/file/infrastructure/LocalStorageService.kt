package demo.drive.file.infrastructure

import demo.drive.common.exception.DriveException
import demo.drive.file.domain.StorageService
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.Resource
import org.springframework.core.io.UrlResource
import org.springframework.stereotype.Service
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.util.UUID

@Service
class LocalStorageService(
    @Value("\${drive.storage.root:./storage}") private val rootPath: String,
) : StorageService {

    override fun store(userId: Long, filename: String, inputStream: InputStream, size: Long): String {
        val ext = filename.substringAfterLast('.', "bin")
        val storagePath = "$userId/${UUID.randomUUID()}.$ext"
        validatePath(storagePath)
        val targetPath = Path.of(rootPath).resolve(storagePath).normalize()
        Files.createDirectories(targetPath.parent)
        inputStream.use { Files.copy(it, targetPath, StandardCopyOption.REPLACE_EXISTING) }
        return storagePath
    }

    override fun load(storagePath: String): Resource {
        validatePath(storagePath)
        val path = Path.of(rootPath).resolve(storagePath).normalize()
        val resource = UrlResource(path.toUri())
        if (!resource.exists() || !resource.isReadable) {
            throw DriveException.invalidRequest("파일을 읽을 수 없습니다.")
        }
        return resource
    }

    override fun delete(storagePath: String) {
        validatePath(storagePath)
        val path = Path.of(rootPath).resolve(storagePath).normalize()
        Files.deleteIfExists(path)
    }

    private fun validatePath(storagePath: String) {
        if (storagePath.contains("..")) {
            throw DriveException.invalidRequest("잘못된 파일 경로입니다.")
        }
    }
}
