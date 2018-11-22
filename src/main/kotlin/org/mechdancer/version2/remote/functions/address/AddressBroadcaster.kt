package org.mechdancer.version2.remote.functions.address

import org.mechdancer.remote.core.protocol.RemotePacket
import org.mechdancer.remote.core.protocol.bytes
import org.mechdancer.version2.dependency.AbstractModule
import org.mechdancer.version2.dependency.hashOf
import org.mechdancer.version2.dependency.must
import org.mechdancer.version2.remote.functions.multicast.MulticastBroadcaster
import org.mechdancer.version2.remote.functions.multicast.MulticastListener
import org.mechdancer.version2.remote.resources.Name
import org.mechdancer.version2.remote.resources.Name.Type.NAME
import org.mechdancer.version2.remote.resources.ServerSockets
import org.mechdancer.version2.remote.resources.UdpCmd.ADDRESS_ACK
import org.mechdancer.version2.remote.resources.UdpCmd.ADDRESS_ASK
import java.net.InetSocketAddress

/**
 * 地址同步机制 2
 * 这个模块用于 TCP 连接的接收者
 * 因此必须具备有效的 TCP 监听套接字和名字，并依赖组播收发
 */
class AddressBroadcaster :
    AbstractModule(),
    MulticastListener {
    private val name by must<Name> { host }
    private val broadcaster by must<MulticastBroadcaster> { host }
    private val servers by must<ServerSockets> { host }

    override val interest = INTEREST

    override fun process(remotePacket: RemotePacket) {
        if (String(remotePacket.payload) == name[NAME])
            servers[0]
                ?.localSocketAddress
                ?.let { it as InetSocketAddress }
                ?.bytes
                ?.let { broadcaster.broadcast(ADDRESS_ACK, it) }
    }

    override fun equals(other: Any?) = other is AddressBroadcaster
    override fun hashCode() = TYPE_HASH

    private companion object {
        val INTEREST = setOf(ADDRESS_ASK)
        val TYPE_HASH = hashOf<AddressBroadcaster>()
    }
}
