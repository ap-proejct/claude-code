package demo.drive.file.fake

import demo.drive.file.domain.StorageService
import org.springframework.core.io.ByteArrayResource
import org.springframework.core.io.Resource
import java.io.InputStream
import java.util.UUID

/**
 * StorageService의 인메모리 Fake 구현체.
 * Service 단위 테스트에서 실제 디스크 접근 없이 사용한다.
 */
class FakeStorageService : StorageService {
    private val store = mutableMapOf<String, ByteArray>()

    override fun store(userId: Long, filename: String, inputStream: InputStream, size: Long): String {
        val ext = filename.substringAfterLast('.', "bin")
        val storagePath = "$userId/${UUID.randomUUID()}.$ext"
        store[storagePath] = inputStream.readBytes()
        return storagePath
    }

    override fun load(storagePath: String): Resource {
        val bytes = store[storagePath] ?: ByteArray(0)
        return ByteArrayResource(bytes)
    }

    override fun delete(storagePath: String) {
        store.remove(storagePath)
    }

    fun clear() = store.clear()
}
