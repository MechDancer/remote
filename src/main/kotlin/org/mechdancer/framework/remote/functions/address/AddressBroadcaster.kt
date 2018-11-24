package org.mechdancer.framework.remote.functions.address

import org.mechdancer.framework.dependency.AbstractModule
import org.mechdancer.framework.dependency.hashOf
import org.mechdancer.framework.dependency.must
import org.mechdancer.framework.remote.functions.multicast.MulticastBroadcaster
import org.mechdancer.framework.remote.functions.multicast.MulticastListener
import org.mechdancer.framework.remote.protocol.RemotePacket
import org.mechdancer.framework.remote.resources.MulticastSockets
import org.mechdancer.framework.remote.resources.Name
import org.mechdancer.framework.remote.resources.Networks
import org.mechdancer.framework.remote.resources.ServerSockets
import org.mechdancer.framework.remote.resources.UdpCmd.ADDRESS_ACK
import org.mechdancer.framework.remote.resources.UdpCmd.ADDRESS_ASK
import java.io.ByteArrayOutputStream

/**
 * 地址同步机制 2
 * 这个模块用于 TCP 连接的接收者
 * 因此必须具备有效的 TCP 监听套接字和名字，并依赖组播收发
 */
class AddressBroadcaster :
    AbstractModule(),
    MulticastListener {
    private val name by must<Name>(host)
    private val networks by must<Networks>(host)
    private val sockets by must<MulticastSockets>(host)
    private val broadcaster by must<MulticastBroadcaster>(host)
    private val servers by must<ServerSockets>(host)

    override val interest = INTEREST

    override fun process(remotePacket: RemotePacket) {
        if (String(remotePacket.payload) != name.value) return
        val addresses = sockets.view.keys
        val port = servers.default.localPort
        ByteArrayOutputStream(4 * (addresses.size + 1))
            .apply {
                for (network in addresses)
                    networks[network]?.let { write(it.address) }
                write(port shr 8)
                write(port)
            }
            .toByteArray()
            .let { broadcaster.broadcast(ADDRESS_ACK, it) }
    }

    override fun equals(other: Any?) = other is AddressBroadcaster
    override fun hashCode() = TYPE_HASH

    private companion object {
        val INTEREST = setOf(ADDRESS_ASK)
        val TYPE_HASH = hashOf<AddressBroadcaster>()
    }
}
