package org.mechdancer.framework.remote.modules.address

import org.mechdancer.framework.dependency.AbstractModule
import org.mechdancer.framework.dependency.hashOf
import org.mechdancer.framework.dependency.must
import org.mechdancer.framework.remote.modules.multicast.MulticastBroadcaster
import org.mechdancer.framework.remote.modules.multicast.MulticastListener
import org.mechdancer.framework.remote.protocol.RemotePacket
import org.mechdancer.framework.remote.resources.Addresses
import org.mechdancer.framework.remote.resources.UdpCmd.ADDRESS_ACK
import org.mechdancer.framework.remote.resources.UdpCmd.ADDRESS_ASK
import java.net.Inet4Address
import java.net.InetAddress
import java.net.InetSocketAddress

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
        if (remotePacket.sender.isBlank()) return
        val (sender, _, _, payload) = remotePacket
        val address = List(payload.size / 4) {
            payload
                .copyOfRange(it * 4, (it + 1) * 4)
                .let(Inet4Address::getByAddress)
        }
            // TODO 与本机网络端口匹配
            .firstOrNull()
            ?: InetAddress.getByAddress(ByteArray(4))
        val port = payload[payload.lastIndex - 1]() shl 8 or payload.last()()
        addresses.update(sender, InetSocketAddress(address, port))
    }

    override fun equals(other: Any?) = other is AddressMonitor
    override fun hashCode() = TYPE_HASH

    private companion object {
        val INTEREST = setOf(ADDRESS_ACK)
        val TYPE_HASH = hashOf<AddressMonitor>()
        operator fun Byte.invoke() = this + if (this >= 0) 0 else 256
    }
}
