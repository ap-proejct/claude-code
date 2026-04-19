package demo.drive.file.domain

import org.springframework.core.io.Resource
import java.io.InputStream

interface StorageService {
    fun store(userId: Long, filename: String, inputStream: InputStream, size: Long): String  // storagePath 반환
    fun load(storagePath: String): Resource
    fun delete(storagePath: String)
}
