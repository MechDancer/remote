package org.mechdancer.framework.remote.resources

import org.mechdancer.framework.dependency.Component
import org.mechdancer.framework.dependency.buildView
import org.mechdancer.framework.dependency.hashOf
import java.util.concurrent.ConcurrentHashMap

/**
 * 成员存在性资源
 */
class Group : Component {
    private val core = ConcurrentHashMap<String, Long>()
    val view = buildView(core)

    fun update(parameter: String, resource: Long?): Long? =
        if (resource != null) core.put(parameter, resource)
        else core.remove(parameter)

    operator fun get(parameter: String) = core[parameter]

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
