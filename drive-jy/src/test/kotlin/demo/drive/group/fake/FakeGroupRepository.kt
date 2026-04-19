package demo.drive.group.fake

import demo.drive.group.domain.Group
import demo.drive.group.domain.GroupRepository
import java.lang.reflect.Field

class FakeGroupRepository : GroupRepository {
    private val store = mutableMapOf<Long, Group>()
    private var sequence = 1L

    override fun findById(id: Long): Group? = store[id]
    override fun findByIdIn(ids: List<Long>): List<Group> = store.values.filter { it.id in ids }
    override fun save(group: Group): Group {
        val id = if (group.id == 0L) sequence++ else group.id
        val saved = setId(group, id)
        store[id] = saved
        return saved
    }
    override fun delete(group: Group) { store.remove(group.id) }

    fun clear() { store.clear(); sequence = 1L }

    private fun setId(group: Group, id: Long): Group {
        val field: Field = Group::class.java.getDeclaredField("id")
        field.isAccessible = true
        field.set(group, id)
        return group
    }
}
