package org.mechdancer.framework.remote.modules.tcpconnection

import org.mechdancer.framework.dependency.AbstractDependent
import org.mechdancer.framework.dependency.hashOf
import org.mechdancer.framework.remote.modules.multicast.MulticastBroadcaster
import org.mechdancer.framework.remote.modules.multicast.MulticastListener
import org.mechdancer.framework.remote.protocol.RemotePacket
import org.mechdancer.framework.remote.resources.Addresses
import org.mechdancer.framework.remote.resources.UdpCmd.ADDRESS_ACK
import org.mechdancer.framework.remote.resources.UdpCmd.ADDRESS_ASK

/**
 * 地址同步机制 1
 * 这个模块用于 TCP 连接的发起者
 * 依赖地址资源和组播收发功能
 * 将发起地址询问并更新地址资源
 */
class PortMonitor : AbstractDependent(), MulticastListener {

    private val broadcast by must { it: MulticastBroadcaster -> it::broadcast }
    private val addresses by must<Addresses>()

    override val interest = INTEREST

    /** 向所有远端发送端口询问 */
    fun askEveryone() = broadcast(ADDRESS_ASK, byteArrayOf())

    /** 向一个名为 [name] 的远端发送端口询问 */
    fun ask(name: String) = broadcast(ADDRESS_ASK, name.toByteArray())

    override fun process(remotePacket: RemotePacket) {
        val (sender, _, payload) = remotePacket

        if (sender.isNotBlank()) // 忽略匿名终端的地址
            addresses[sender] = payload(0) shl 8 or payload(1)
    }

    override fun equals(other: Any?) = other is PortMonitor
    override fun hashCode() = TYPE_HASH

    private companion object {
        val INTEREST = setOf(ADDRESS_ACK.id)
        val TYPE_HASH = hashOf<PortMonitor>()
        operator fun ByteArray.invoke(n: Int) = get(n).toInt() and 0xff
    }
}
