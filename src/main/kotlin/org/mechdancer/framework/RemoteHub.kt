package org.mechdancer.framework

import org.mechdancer.framework.dependency.plusAssign
import org.mechdancer.framework.dependency.scope
import org.mechdancer.framework.remote.functions.GroupMonitor
import org.mechdancer.framework.remote.functions.address.AddressBroadcaster
import org.mechdancer.framework.remote.functions.address.AddressMonitor
import org.mechdancer.framework.remote.functions.multicast.CommonMulticast
import org.mechdancer.framework.remote.functions.multicast.MulticastBroadcaster
import org.mechdancer.framework.remote.functions.multicast.MulticastReceiver
import org.mechdancer.framework.remote.functions.multicast.PacketSlicer
import org.mechdancer.framework.remote.functions.tcpconnection.ShortConnectionClient
import org.mechdancer.framework.remote.functions.tcpconnection.ShortConnectionServer
import org.mechdancer.framework.remote.resources.*
import java.net.InetAddress
import java.net.InetSocketAddress
import java.util.*

/**
 * 远程终端
 */
class RemoteHub(
    name: String? = null,
    newMemberDetected: (String) -> Unit = {},
    broadcastReceived: (String, ByteArray) -> Unit = { _, _ -> }
) {
    // UDP 依赖项

    // 组成员资源
    private val group = Group()
    // 组成员管理
    private val monitor = GroupMonitor(newMemberDetected)

    private val networks = Networks()
    // 组播套接字
    private val _sockets = MulticastSockets(ADDRESS)
    // 组播广播器
    private val broadcaster = MulticastBroadcaster()
    // 组播接收器
    private val receiver = MulticastReceiver()
    // 通用组播收发
    private val common = CommonMulticast(broadcastReceived)
    // 组播分片协议
    private val slicer = PacketSlicer()

    // TCP 依赖项

    // 组地址资源
    private val addresses = Addresses()
    // 监听套接字资源
    private val serverSockets = ServerSockets()
    // 组地址同步器
    private val synchronizer1 = AddressBroadcaster()
    private val synchronizer2 = AddressMonitor()
    // 短连接建立
    private val client = ShortConnectionClient()
    private val server = ShortConnectionServer()

    val hub = scope {
        // 名字
        this += Name(name ?: randomName())

        // 组成员管理
        this += group   // 成员存在性资源
        this += monitor // 组成员管理

        // 组播
        this += _sockets    // 组播套接字资源
        this += broadcaster // 组播发送
        this += receiver    // 组播接收
        this += common      // 通用组播收发
        this += slicer      // 组播分片协议

        // TCP 地址
        this += addresses     // 地址资源
        this += serverSockets // 监听套接字资源
        this += synchronizer1 // 组地址同步器（答）
        this += synchronizer2 // 组地址同步器（问）

        // TCP 短连接
        this += server // 服务端
        this += client // 客户端

        // 打开组播发送端
        networks.scan()
        networks.view.forEach { network, _ -> _sockets[network] }
    }

    // access

    /**
     * 查看超时时间内出现的成员
     */
    infix fun membersBy(timeout: Int) = group[timeout]

    /**
     * 查看所有打开的组播套接字
     */
    val sockets get() = _sockets.view

    // function

    /**
     * 请求自证存在性
     */
    fun yell() = monitor.yell()

    /**
     * 发送通用广播
     */
    infix fun broadcast(payload: ByteArray) = common broadcast payload

    // service

    /**
     * 阻塞等待 UDP 报文
     */
    operator fun invoke() = receiver()

    /**
     * 阻塞等待 TCP 连接
     */
    fun accept() = server()

    private companion object {
        val ADDRESS = InetSocketAddress(InetAddress.getByName("238.88.88.88"), 23333)
        fun randomName() = "RemoteHub[${UUID.randomUUID()}]"
    }
}
