package org.mechdancer.version2.remote.functions

import org.mechdancer.remote.core.protocol.RemotePacket
import org.mechdancer.remote.core.protocol.bytes
import org.mechdancer.version2.dependency.AbstractModule
import org.mechdancer.version2.hashOf
import org.mechdancer.version2.must
import org.mechdancer.version2.remote.resources.Name
import org.mechdancer.version2.remote.resources.Name.Type.NAME
import org.mechdancer.version2.remote.resources.ServerSockets
import org.mechdancer.version2.remote.resources.UdpCmd
import org.mechdancer.version2.remote.resources.UdpCmd.ADDRESS_ACK
import org.mechdancer.version2.remote.resources.UdpCmd.ADDRESS_ASK
import java.net.InetSocketAddress

/**
 * 地址同步器
 */
class AddressSynchronizer :
    AbstractModule(),
    MulticastListener {
    private val name by lazy { host.must<Name>() }
    private val servers by lazy { host.must<ServerSockets>() }
    private val broadcaster by lazy { host.must<MulticastBroadcaster>() }

    fun ask(name: String) = broadcaster.broadcast(ADDRESS_ASK, name.toByteArray())

    override fun process(remotePacket: RemotePacket) {
        val (cmd, sender, _, payload) = remotePacket
        if (sender.isBlank()) return
        when (UdpCmd[cmd]) {
            ADDRESS_ASK -> {
                if (String(payload) == name[NAME])
                    broadcaster.broadcast(
                        ADDRESS_ACK,
                        (servers[0]!!.localSocketAddress as InetSocketAddress).bytes
                    )
            }
            ADDRESS_ACK -> {

            }
            else        -> Unit
        }
    }

    override fun equals(other: Any?) = other is AddressSynchronizer
    override fun hashCode() = TYPE_HASH

    private companion object {
        val TYPE_HASH = hashOf<AddressSynchronizer>()
    }
}
