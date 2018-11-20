package org.mechdancer.version2.dependency.functions.basic

import org.mechdancer.remote.core.internal.Command
import org.mechdancer.remote.core.protocol.RemotePackage
import org.mechdancer.version2.dependency.Dependency
import org.mechdancer.version2.dependency.functions.basic.GroupMonitor.Cmd.Ack
import org.mechdancer.version2.dependency.functions.basic.GroupMonitor.Cmd.Ask
import org.mechdancer.version2.dependency.resource.basic.Group
import org.mechdancer.version2.hashOf
import org.mechdancer.version2.must

/**
 * 组成员管理
 */
class GroupMonitor(
    private val detected: (String) -> Unit
) : MulticastListener() {
    private lateinit var broadcaster: MulticastBroadcaster
    private lateinit var group: Group

    override val dependencies
        get() = setOf(group, broadcaster)

    override fun loadDependencies(dependency: Iterable<Dependency>) {
        group = dependency.must()
        broadcaster = dependency.must()
    }

    fun yell() = broadcaster.broadcast(Ask)

    override fun invoke(remotePackage: RemotePackage) {
        val (id, name, _) = remotePackage
        group.update(name, now()) ?: detected(name)
        if (id == Ask.id) broadcaster.broadcast(Ack)
    }

    override fun equals(other: Any?) = other is GroupMonitor
    override fun hashCode() = TYPE_HASH

    enum class Cmd(override val id: Byte) : Command {
        Ask(0), Ack(1);
    }

    private companion object {
        val TYPE_HASH = hashOf<GroupMonitor>()
        fun now() = System.currentTimeMillis()
    }
}
