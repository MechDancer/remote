package org.mechdancer.framework.remote.functions

import org.mechdancer.framework.dependency.AbstractModule
import org.mechdancer.framework.dependency.hashOf
import org.mechdancer.framework.dependency.must
import org.mechdancer.framework.remote.functions.multicast.MulticastBroadcaster
import org.mechdancer.framework.remote.functions.multicast.MulticastListener
import org.mechdancer.framework.remote.protocol.RemotePacket
import org.mechdancer.framework.remote.resources.Group
import org.mechdancer.framework.remote.resources.UdpCmd
import org.mechdancer.framework.remote.resources.UdpCmd.YELL_ACK
import org.mechdancer.framework.remote.resources.UdpCmd.YELL_ASK

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

    override val interest = INTEREST

    override fun process(remotePacket: RemotePacket) {
        val (id, name, _) = remotePacket

        if (name.isNotBlank()) // 非匿名则保存名字
            group.update(name, now()) ?: detected(name)

        if (id == UdpCmd.YELL_ASK.id) // 回应询问
            broadcaster.broadcast(UdpCmd.YELL_ACK)
    }

    override fun equals(other: Any?) = other is GroupMonitor
    override fun hashCode() = TYPE_HASH

    private companion object {
        val INTEREST = setOf(YELL_ASK, YELL_ACK)
        val TYPE_HASH = hashOf<GroupMonitor>()
        fun now() = System.currentTimeMillis()
    }
}
