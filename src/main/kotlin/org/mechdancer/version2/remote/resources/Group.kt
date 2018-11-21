package org.mechdancer.version2.remote.resources

import org.mechdancer.version2.dependency.ResourceMemory
import org.mechdancer.version2.hashOf
import java.util.concurrent.ConcurrentHashMap

/**
 * 成员存在性资源
 */
class Group : ResourceMemory<String, Long> {
    private val core = ConcurrentHashMap<String, Long>()

    /**
     * 浏览所有成员及其生存时间
     */
    val view = object : Map<String, Long> by core {}

    override fun update(parameter: String, resource: Long?): Long? =
        if (resource != null) core.put(parameter, resource)
        else core.remove(parameter)

    override operator fun get(parameter: String) = core[parameter]

    /**
     * 获取最后出现时间短于超时时间的成员
     */
    operator fun get(timeout: Int): List<String> {
        val now = System.currentTimeMillis()
        return core.mapNotNull { (name, time) -> name.takeIf { now - time < timeout } }
    }

    override fun equals(other: Any?) = other is Group
    override fun hashCode() = TYPE_HASH

    private companion object {
        val TYPE_HASH = hashOf<Group>()
    }
}
