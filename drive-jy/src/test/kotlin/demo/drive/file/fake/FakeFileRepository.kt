package demo.drive.file.fake

import demo.drive.file.domain.File
import demo.drive.file.domain.FileRepository
import java.lang.reflect.Field
import java.time.Instant

/**
 * FileRepository의 인메모리 Fake 구현체.
 * Service 단위 테스트에서 Spring 없이 사용한다.
 */
class FakeFileRepository : FileRepository {
    private val store = mutableMapOf<Long, File>()
    private var sequence = 1L

    override fun save(file: File): File {
        val id = if (file.id == 0L) sequence++ else file.id
        val saved = setId(file, id)
        store[id] = saved
        return saved
    }

    override fun findById(id: Long): File? = store[id]

    override fun findByOwnerIdAndParentIdIsNullAndIsTrashedFalse(ownerId: Long): List<File> =
        store.values.filter { it.ownerId == ownerId && it.parentId == null && !it.isTrashed }

    override fun findByParentIdAndIsTrashedFalse(parentId: Long): List<File> =
        store.values.filter { it.parentId == parentId && !it.isTrashed }

    override fun findByParentId(parentId: Long): List<File> =
        store.values.filter { it.parentId == parentId }

    override fun findByOwnerIdAndIsTrashedTrue(ownerId: Long): List<File> =
        store.values.filter { it.ownerId == ownerId && it.isTrashed }

    override fun findByIsTrashedTrueAndTrashedAtBefore(threshold: Instant): List<File> =
        store.values.filter { it.isTrashed && it.trashedAt != null && it.trashedAt!! < threshold }

    override fun findByOwnerIdAndIsStarredTrueAndIsTrashedFalse(ownerId: Long): List<File> =
        store.values.filter { it.ownerId == ownerId && it.isStarred && !it.isTrashed }

    override fun findAllByIdIn(ids: List<Long>): List<File> =
        store.values.filter { it.id in ids }

    override fun delete(file: File) {
        store.remove(file.id)
    }

    fun clear() {
        store.clear()
        sequence = 1L
    }

    private fun setId(file: File, id: Long): File {
        val field: Field = File::class.java.getDeclaredField("id")
        field.isAccessible = true
        field.set(file, id)
        return file
    }
}
