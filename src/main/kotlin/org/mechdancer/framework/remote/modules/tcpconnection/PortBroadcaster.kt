package org.mechdancer.framework.remote.modules.tcpconnection

import org.mechdancer.framework.dependency.AbstractModule
import org.mechdancer.framework.dependency.hashOf
import org.mechdancer.framework.dependency.must
import org.mechdancer.framework.remote.modules.multicast.MulticastBroadcaster
import org.mechdancer.framework.remote.modules.multicast.MulticastListener
import org.mechdancer.framework.remote.protocol.RemotePacket
import org.mechdancer.framework.remote.resources.Name
import org.mechdancer.framework.remote.resources.ServerSockets
import org.mechdancer.framework.remote.resources.UdpCmd.ADDRESS_ACK
import org.mechdancer.framework.remote.resources.UdpCmd.ADDRESS_ASK

/**
 * 端口同步机制 2
 * 这个模块用于 TCP 连接的接收者
 * 因此必须具备有效的 TCP 监听套接字和名字，并依赖组播收发
 */
class PortBroadcaster : AbstractModule(), MulticastListener {

    private val name by must<Name>(host)
    private val broadcaster by must<MulticastBroadcaster>(host)
    private val servers by must<ServerSockets>(host)

    override val interest = INTEREST

    override fun process(remotePacket: RemotePacket) {
        if (String(remotePacket.payload) != name.value) return

        val port = servers.default.localPort
        broadcaster.broadcast(
            ADDRESS_ACK,
            byteArrayOf((port shr 8).toByte(), port.toByte())
        )
    }

    override fun equals(other: Any?) = other is PortBroadcaster
    override fun hashCode() = TYPE_HASH

    private companion object {
        val INTEREST = setOf(ADDRESS_ASK)
        val TYPE_HASH = hashOf<PortBroadcaster>()
    }
}
