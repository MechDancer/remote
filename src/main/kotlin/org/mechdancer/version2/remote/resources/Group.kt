package org.mechdancer.version2.remote.resources

import org.mechdancer.version2.dependency.ResourceMemory
import org.mechdancer.version2.hashOf
import java.util.concurrent.ConcurrentHashMap

/**
 * 成员存在性资源
 */
class Group : ResourceMemory<String, Long> {
    private val core = ConcurrentHashMap<String, Long>()
    val view = object : Map<String, Long> by core {}

    override fun update(parameter: String, resource: Long?): Long? =
        if (resource != null) core.put(parameter, resource)
        else core.remove(parameter)

    override operator fun get(parameter: String) = core[parameter]

    override fun equals(other: Any?) = other is Group
    override fun hashCode() = TYPE_HASH

    private companion object {
        val TYPE_HASH = hashOf<Group>()
    }
}
