package demo.drive.group.domain

interface GroupRepository {
    fun findById(id: Long): Group?
    fun findByIdIn(ids: List<Long>): List<Group>
    fun save(group: Group): Group
    fun delete(group: Group)
}
