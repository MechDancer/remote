package org.mechdancer.version2.remote.functions

import org.mechdancer.remote.core.protocol.RemotePacket
import org.mechdancer.version2.dependency.AbstractModule
import org.mechdancer.version2.hashOf
import org.mechdancer.version2.must
import org.mechdancer.version2.remote.resources.Addresses
import org.mechdancer.version2.remote.resources.UdpCmd.ADDRESS_ACK
import org.mechdancer.version2.remote.resources.UdpCmd.ADDRESS_ASK

/**
 * 地址同步器
 */
class AddressSynchronizer :
    AbstractModule(),
    MulticastListener {
    private val addresses by lazy { host.must<Addresses>() }
    private val broadcaster by lazy { host.must<MulticastBroadcaster>() }

    fun ask(name: String) = broadcaster.broadcast(ADDRESS_ASK, name.toByteArray())

    override fun process(remotePacket: RemotePacket) {
        val (cmd, name, _, payload) = remotePacket
        if (cmd != ADDRESS_ACK.id || name.isBlank()) return
        // addresses.update(name, inetSocketAddress())
    }

    override fun equals(other: Any?) = other is AddressSynchronizer
    override fun hashCode() = TYPE_HASH

    private companion object {
        val TYPE_HASH = hashOf<AddressSynchronizer>()
    }
}
