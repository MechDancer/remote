package org.mechdancer.version2

import org.mechdancer.remote.network.MULTICAST_FILTERS
import org.mechdancer.remote.network.WIRELESS_FIRST
import org.mechdancer.remote.network.filterNetwork
import org.mechdancer.version2.dependency.functions.basic.CommonMultacaster
import org.mechdancer.version2.dependency.functions.basic.GroupMonitor
import org.mechdancer.version2.dependency.functions.basic.MulticastBroadcaster
import org.mechdancer.version2.dependency.functions.basic.MulticastReceiver
import org.mechdancer.version2.dependency.resources.basic.Group
import org.mechdancer.version2.dependency.resources.basic.MulticastSockets
import org.mechdancer.version2.dependency.resources.basic.Name
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.util.*

class RemoteHub(
    name: String? = null,
    network: NetworkInterface? = null,
    newMemberDetected: (String) -> Unit = {},
    broadcastReceived: (String, ByteArray) -> Unit = { _, _ -> }
) {
    private val group = Group()
    private val broadcaster = MulticastBroadcaster()
    private val receiver = MulticastReceiver()
    private val common = CommonMultacaster(broadcastReceived)

    private val hub = buildHub {
        // 名字
        this += Name(name ?: "Hub[${UUID.randomUUID()}]")

        // 组成员管理
        this += group
        this += GroupMonitor(newMemberDetected)

        // 组播
        val sockets = MulticastSockets(ADDRESS)
        this += sockets     // 组播套接字管理
        this += broadcaster // 组播发送
        this += receiver    // 组播接收
        this += common      // 通用组播收发

        // 选网
        val best = network
            ?: filterNetwork(MULTICAST_FILTERS, WIRELESS_FIRST)
                .let(Collection<NetworkInterface>::firstOrNull)
            ?: throw RuntimeException("no available network")

        sockets[best]

        sync()
    }

    infix fun broadcast(payload: ByteArray) = common broadcast payload
    operator fun invoke() = receiver.invoke()

    private companion object {
        val ADDRESS = InetSocketAddress(InetAddress.getByName("238.88.88.88"), 23333)
    }
}
