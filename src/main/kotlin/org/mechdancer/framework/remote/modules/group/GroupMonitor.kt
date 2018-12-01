package org.mechdancer.framework.remote.modules.group

import org.mechdancer.framework.dependency.AbstractDependent
import org.mechdancer.framework.dependency.hashOf
import org.mechdancer.framework.remote.modules.multicast.MulticastBroadcaster
import org.mechdancer.framework.remote.modules.multicast.MulticastListener
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
) : AbstractDependent(), MulticastListener {
    private val group by must<Group>()
    private val broadcaster by maybe<MulticastBroadcaster>()

    fun yell() = broadcaster?.broadcast(UdpCmd.YELL_ASK)

    override val interest = INTEREST

    override fun process(remotePacket: RemotePacket) {
        val (name, cmd) = remotePacket

        if (name.isNotBlank()) // 非匿名则保存名字
            group.update(name, now()) ?: detected(name)

        if (cmd == UdpCmd.YELL_ASK.id) // 回应询问
            broadcaster?.broadcast(UdpCmd.YELL_ACK)
    }

    override fun equals(other: Any?) = other is GroupMonitor
    override fun hashCode() = TYPE_HASH

    private companion object {
        val INTEREST = setOf(YELL_ASK.id, YELL_ACK.id)
        val TYPE_HASH = hashOf<GroupMonitor>()
        fun now() = System.currentTimeMillis()
    }
}
