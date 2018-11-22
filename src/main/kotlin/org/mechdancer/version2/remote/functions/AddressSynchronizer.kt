package org.mechdancer.version2.remote.functions

import org.mechdancer.remote.core.protocol.RemotePacket
import org.mechdancer.remote.core.protocol.bytes
import org.mechdancer.remote.core.protocol.readInetSocketAddress
import org.mechdancer.version2.dependency.AbstractModule
import org.mechdancer.version2.dependency.hashOf
import org.mechdancer.version2.dependency.maybe
import org.mechdancer.version2.dependency.must
import org.mechdancer.version2.remote.resources.Addresses
import org.mechdancer.version2.remote.resources.Name
import org.mechdancer.version2.remote.resources.Name.Type.NAME
import org.mechdancer.version2.remote.resources.ServerSockets
import org.mechdancer.version2.remote.resources.UdpCmd.ADDRESS_ACK
import org.mechdancer.version2.remote.resources.UdpCmd.ADDRESS_ASK
import org.mechdancer.version2.remote.streams.SimpleInputStream
import java.net.InetSocketAddress

/**
 * 地址同步器
 */
class AddressSynchronizer :
    AbstractModule(),
    MulticastListener {
    private val name by must<Name> { host }
    private val addresses by must<Addresses> { host }
    private val broadcaster by must<MulticastBroadcaster> { host }

    /**
     * 本机可以不具备监听套接字
     */
    private val servers by maybe<ServerSockets> { host }

    /**
     * 向一个远端发送地址询问
     */
    infix fun ask(name: String) =
        broadcaster.broadcast(ADDRESS_ASK, name.toByteArray())

    override fun process(remotePacket: RemotePacket) {
        val (cmd, sender, _, payload) = remotePacket
        if (sender.isBlank()) return
        when (cmd) {
            ADDRESS_ASK.id ->
                if (String(payload) == name[NAME])
                    servers
                        ?.get(0)
                        ?.localSocketAddress
                        ?.let { it as InetSocketAddress }
                        ?.bytes
                        ?.let { broadcaster.broadcast(ADDRESS_ACK, it) }
                else Unit
            ADDRESS_ACK.id ->
                SimpleInputStream(payload)
                    .readInetSocketAddress()
                    .let { addresses.update(sender, it) }
            else           -> Unit
        }
    }

    override fun equals(other: Any?) = other is AddressSynchronizer
    override fun hashCode() = TYPE_HASH

    private companion object {
        val TYPE_HASH = hashOf<AddressSynchronizer>()
    }
}
