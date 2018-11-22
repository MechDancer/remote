package org.mechdancer.version2

import org.mechdancer.remote.network.MULTICAST_FILTERS
import org.mechdancer.remote.network.WIRELESS_FIRST
import org.mechdancer.remote.network.filterNetwork
import org.mechdancer.version2.remote.functions.GroupMonitor
import org.mechdancer.version2.remote.functions.MulticastBroadcaster
import org.mechdancer.version2.remote.functions.MulticastReceiver
import org.mechdancer.version2.remote.functions.commons.CommonMulticast
import org.mechdancer.version2.remote.resources.Group
import org.mechdancer.version2.remote.resources.MulticastSockets
import org.mechdancer.version2.remote.resources.Name
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.util.*

/**
 * 远程终端
 */
class RemoteHub(
    name: String? = null,
    network: NetworkInterface? = null,
    newMemberDetected: (String) -> Unit = {},
    broadcastReceived: (String, ByteArray) -> Unit = { _, _ -> }
) {
    private val group = Group()
    private val broadcaster = MulticastBroadcaster()
    private val receiver = MulticastReceiver()
    private val common = CommonMulticast(broadcastReceived)

    private val hub = buildHub {
        // 名字
        this += Name(name ?: "DynamicScope[${UUID.randomUUID()}]")

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

    /**
     * 查看超时时间内出现的成员
     */
    fun membersBy(timeout: Int) = group[timeout]

    /**
     * 发送通用广播
     */
    infix fun broadcast(payload: ByteArray) = common broadcast payload

    /**
     * 启动阻塞接收
     */
    operator fun invoke() = receiver.invoke()

    private companion object {
        val ADDRESS = InetSocketAddress(InetAddress.getByName("238.88.88.88"), 23333)
    }
}
