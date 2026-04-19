package demo.drive.file.domain

interface FileRepository {
    fun findById(id: Long): File?
    fun findByOwnerIdAndParentIdIsNullAndIsTrashedFalse(ownerId: Long): List<File>
    fun findByParentIdAndIsTrashedFalse(parentId: Long): List<File>
    fun findByParentId(parentId: Long): List<File>
    fun findByOwnerIdAndIsTrashedTrue(ownerId: Long): List<File>
    fun findByIsTrashedTrueAndTrashedAtBefore(threshold: java.time.Instant): List<File>
    fun findByOwnerIdAndIsStarredTrueAndIsTrashedFalse(ownerId: Long): List<File>
    fun findAllByIdIn(ids: List<Long>): List<File>
    fun save(file: File): File
    fun delete(file: File)
}
