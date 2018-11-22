package org.mechdancer.version2.remote.functions

import org.mechdancer.remote.core.protocol.RemotePacket
import org.mechdancer.version2.dependency.AbstractModule
import org.mechdancer.version2.dependency.hashOf
import org.mechdancer.version2.dependency.must
import org.mechdancer.version2.remote.resources.Group
import org.mechdancer.version2.remote.resources.UdpCmd

/**
 * 组成员管理
 * @param detected 发现新成员的回调
 */
class GroupMonitor(
    private val detected: (String) -> Unit = {}
) : AbstractModule(), MulticastListener {
    private val group by must<Group> { host }
    private val broadcaster by must<MulticastBroadcaster> { host }

    fun yell() = broadcaster.broadcast(UdpCmd.YELL_ASK)

    override fun process(remotePacket: RemotePacket) {
        val (id, name, _) = remotePacket
        group.update(name, now()) ?: detected(name)
        if (id == UdpCmd.YELL_ASK.id) broadcaster.broadcast(UdpCmd.YELL_ACK)
    }

    override fun equals(other: Any?) = other is GroupMonitor
    override fun hashCode() = TYPE_HASH

    private companion object {
        val TYPE_HASH = hashOf<GroupMonitor>()
        fun now() = System.currentTimeMillis()
    }
}
