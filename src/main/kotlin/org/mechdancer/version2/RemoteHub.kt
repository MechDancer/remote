package org.mechdancer.version2

import org.mechdancer.remote.network.MULTICAST_FILTERS
import org.mechdancer.remote.network.WIRELESS_FIRST
import org.mechdancer.remote.network.filterNetwork
import org.mechdancer.version2.dependency.buildScope
import org.mechdancer.version2.dependency.plusAssign
import org.mechdancer.version2.remote.functions.AddressSynchronizer
import org.mechdancer.version2.remote.functions.GroupMonitor
import org.mechdancer.version2.remote.functions.MulticastBroadcaster
import org.mechdancer.version2.remote.functions.MulticastReceiver
import org.mechdancer.version2.remote.functions.commons.CommonMulticast
import org.mechdancer.version2.remote.resources.*
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.NetworkInterface

/**
 * 远程终端
 */
class RemoteHub(
    name: String? = null,
    network: NetworkInterface? = null,
    newMemberDetected: (String) -> Unit = {},
    broadcastReceived: (String, ByteArray) -> Unit = { _, _ -> }
) {
    // UDP 依赖项

    // 组成员资源
    private val group = Group()
    // 组成员管理
    private val monitor = GroupMonitor(newMemberDetected)

    // 组播套接字
    private val sockets = MulticastSockets(ADDRESS)
    // 组播广播器
    private val broadcaster = MulticastBroadcaster()
    // 组播接收器
    private val receiver = MulticastReceiver()
    // 通用组播收发
    private val common = CommonMulticast(broadcastReceived)

    // TCP 依赖项

    // 组地址资源
    private val address = Addresses()
    // 监听套接字资源
    private val serverSockets = ServerSockets()
    // 组地址同步器
    private val synchronizer = AddressSynchronizer()

    val hub = buildScope {
        // 名字
        this += Name()

        // 组成员管理
        this += group   // 成员存在性资源
        this += monitor // 组成员管理

        // 组播
        this += sockets     // 组播套接字管理
        this += broadcaster // 组播发送
        this += receiver    // 组播接收
        this += common      // 通用组播收发

        //TCP
        this += address       // 组地址资源
        this += serverSockets // 监听套接字资源
        this += synchronizer  // 组地址同步器

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
