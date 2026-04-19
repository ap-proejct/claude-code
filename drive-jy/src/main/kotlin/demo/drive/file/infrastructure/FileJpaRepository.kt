package demo.drive.file.infrastructure

import demo.drive.file.domain.File
import demo.drive.file.domain.FileRepository
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.time.Instant

interface FileSpringDataRepository : JpaRepository<File, Long> {
    fun findByOwnerIdAndParentIdIsNullAndIsTrashedFalse(ownerId: Long): List<File>
    fun findByParentIdAndIsTrashedFalse(parentId: Long): List<File>
    fun findByParentId(parentId: Long): List<File>
    fun findByOwnerIdAndIsTrashedTrue(ownerId: Long): List<File>
    fun findByIsTrashedTrueAndTrashedAtBefore(threshold: Instant): List<File>
    fun findByOwnerIdAndIsStarredTrueAndIsTrashedFalse(ownerId: Long): List<File>
    fun findAllByIdIn(ids: List<Long>): List<File>
}

@Repository
class FileJpaRepository(
    private val jpaRepo: FileSpringDataRepository,
) : FileRepository {
    override fun findById(id: Long): File? = jpaRepo.findById(id).orElse(null)
    override fun findByOwnerIdAndParentIdIsNullAndIsTrashedFalse(ownerId: Long): List<File> =
        jpaRepo.findByOwnerIdAndParentIdIsNullAndIsTrashedFalse(ownerId)
    override fun findByParentIdAndIsTrashedFalse(parentId: Long): List<File> =
        jpaRepo.findByParentIdAndIsTrashedFalse(parentId)
    override fun findByParentId(parentId: Long): List<File> = jpaRepo.findByParentId(parentId)
    override fun findByOwnerIdAndIsTrashedTrue(ownerId: Long): List<File> =
        jpaRepo.findByOwnerIdAndIsTrashedTrue(ownerId)
    override fun findByIsTrashedTrueAndTrashedAtBefore(threshold: Instant): List<File> =
        jpaRepo.findByIsTrashedTrueAndTrashedAtBefore(threshold)
    override fun findByOwnerIdAndIsStarredTrueAndIsTrashedFalse(ownerId: Long): List<File> =
        jpaRepo.findByOwnerIdAndIsStarredTrueAndIsTrashedFalse(ownerId)
    override fun findAllByIdIn(ids: List<Long>): List<File> =
        jpaRepo.findAllByIdIn(ids)
    override fun save(file: File): File = jpaRepo.save(file)
    override fun delete(file: File) = jpaRepo.delete(file)
}
