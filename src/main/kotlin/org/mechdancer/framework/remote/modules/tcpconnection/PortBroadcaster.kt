package org.mechdancer.framework.remote.modules.tcpconnection

import org.mechdancer.framework.dependency.AbstractDependent
import org.mechdancer.framework.dependency.hashOf
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
class PortBroadcaster : AbstractDependent(), MulticastListener {

    private val name by must { it: Name -> it.field }
    private val broadcast by must { it: MulticastBroadcaster -> it::broadcast }
    private val port by must { it: ServerSockets -> it.default.localPort }

    override val interest = INTEREST

    override fun process(remotePacket: RemotePacket) {
        if (remotePacket.payload.isEmpty() || String(remotePacket.payload) == name)
            broadcast(ADDRESS_ACK, byteArrayOf((port shr 8).toByte(), port.toByte()))
    }

    override fun equals(other: Any?) = other is PortBroadcaster
    override fun hashCode() = TYPE_HASH

    private companion object {
        val INTEREST = setOf(ADDRESS_ASK.id)
        val TYPE_HASH = hashOf<PortBroadcaster>()
    }
}
