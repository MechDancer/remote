package org.mechdancer.framework.remote.functions.address

import org.mechdancer.framework.dependency.AbstractModule
import org.mechdancer.framework.dependency.hashOf
import org.mechdancer.framework.dependency.must
import org.mechdancer.framework.remote.functions.multicast.MulticastBroadcaster
import org.mechdancer.framework.remote.functions.multicast.MulticastListener
import org.mechdancer.framework.remote.protocol.RemotePacket
import org.mechdancer.framework.remote.protocol.inetSocketAddress
import org.mechdancer.framework.remote.resources.Addresses
import org.mechdancer.framework.remote.resources.UdpCmd.ADDRESS_ACK
import org.mechdancer.framework.remote.resources.UdpCmd.ADDRESS_ASK

/**
 * 地址同步机制 1
 * 这个模块用于 TCP 连接的发起者
 * 依赖地址资源和组播收发功能
 * 将发起地址询问，更新地址资源
 */
class AddressMonitor :
    AbstractModule(),
    MulticastListener {

    private val addresses by must<Addresses>(host)
    private val broadcaster by must<MulticastBroadcaster>(host)

    override val interest = INTEREST

    /**
     * 向一个远端发送地址询问
     */
    infix fun ask(name: String) =
        broadcaster.broadcast(ADDRESS_ASK, name.toByteArray())

    override fun process(remotePacket: RemotePacket) {
        val (_, sender, _, payload) = remotePacket
        if (!sender.isBlank())
            addresses.update(sender, inetSocketAddress(payload))
    }

    override fun equals(other: Any?) = other is AddressMonitor
    override fun hashCode() = TYPE_HASH

    private companion object {
        val INTEREST = setOf(ADDRESS_ACK)
        val TYPE_HASH = hashOf<AddressMonitor>()
    }
}
