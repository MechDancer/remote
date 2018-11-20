package org.mechdancer.version2.dependency.functions.basic

import org.mechdancer.remote.core.internal.Command
import org.mechdancer.remote.core.protocol.RemotePackage
import org.mechdancer.version2.dependency.AbstractModule
import org.mechdancer.version2.dependency.functions.basic.GroupMonitor.Cmd.Ack
import org.mechdancer.version2.dependency.functions.basic.GroupMonitor.Cmd.Ask
import org.mechdancer.version2.dependency.resources.basic.Group
import org.mechdancer.version2.hashOf
import org.mechdancer.version2.maybe
import org.mechdancer.version2.must

/**
 * 组成员管理
 */
class GroupMonitor(
    private val detected: (String) -> Unit
) : AbstractModule(), MulticastListener {
    private var broadcaster: MulticastBroadcaster? = null
    private var group: Group? = null

    override val dependencies
        get() = setOf(MulticastBroadcaster::class, Group::class)

    override fun sync() {
        broadcaster = host.maybe()
        group = host.maybe()
    }

    fun yell() {
        broadcaster ?: run { broadcaster = host.must(); broadcaster!! }
        broadcaster!!.broadcast(Ask)
    }

    override fun process(remotePackage: RemotePackage) {
        broadcaster ?: run { broadcaster = host.must() }
        group ?: run { group = host.must() }

        val (id, name, _) = remotePackage
        group!!.update(name, now()) ?: detected(name)
        if (id == Ask.id) broadcaster!!.broadcast(Ack)
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
