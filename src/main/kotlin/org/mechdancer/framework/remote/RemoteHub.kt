package org.mechdancer.framework.remote

import org.mechdancer.framework.dependency.Dependency
import org.mechdancer.framework.dependency.plusAssign
import org.mechdancer.framework.dependency.scope
import org.mechdancer.framework.remote.modules.group.GroupMonitor
import org.mechdancer.framework.remote.modules.multicast.MulticastBroadcaster
import org.mechdancer.framework.remote.modules.multicast.MulticastReceiver
import org.mechdancer.framework.remote.modules.multicast.PacketSlicer
import org.mechdancer.framework.remote.modules.tcpconnection.PortBroadcaster
import org.mechdancer.framework.remote.modules.tcpconnection.PortMonitor
import org.mechdancer.framework.remote.modules.tcpconnection.ShortConnectionClient
import org.mechdancer.framework.remote.modules.tcpconnection.ShortConnectionServer
import org.mechdancer.framework.remote.resources.*
import java.net.InetAddress
import java.net.InetSocketAddress
import java.util.*

/**
 * 远程终端
 */
class RemoteHub(
    name: String?,
    newMemberDetected: (String) -> Unit,
    additional: Iterable<Dependency>
) {
    // UDP 依赖项

    // 组成员资源
    private val group = Group()
    // 组成员管理
    private val monitor = GroupMonitor(newMemberDetected)

    // 网络接口资源
    private val networks = Networks()
    // 组播套接字
    private val sockets = MulticastSockets(ADDRESS)
    // 组播发送器
    private val broadcaster = MulticastBroadcaster()
    // 组播接收器
    private val receiver = MulticastReceiver()
    // 组播分片协议
    private val slicer = PacketSlicer()

    // TCP 依赖项

    // 组地址资源
    private val addresses = Addresses()
    // 监听套接字资源
    private val servers = ServerSockets()
    // 组地址同步器
    private val synchronizer1 = PortBroadcaster()
    private val synchronizer2 = PortMonitor()
    // 短连接建立
    private val client = ShortConnectionClient()
    private val server = ShortConnectionServer()

    private val scope = scope {
        // 名字
        this += Name(name ?: randomName())

        // 组成员管理
        this += group   // 成员存在性资源
        this += monitor // 组成员管理

        // 组播
        this += networks    // 本机网络端口资源
        this += sockets     // 组播套接字资源
        this += broadcaster // 组播发送
        this += receiver    // 组播接收
        this += slicer      // 组播分片协议

        // TCP 地址
        this += addresses     // 地址资源
        this += servers       // 监听套接字资源
        this += synchronizer1 // 组地址同步器（答）
        this += synchronizer2 // 组地址同步器（问）

        // TCP 短连接
        this += server // 服务端
        this += client // 客户端

        for (dependency in additional)
            this += dependency

        sync()
    }

    // access

    /** 浏览全部依赖项 */
    val modules get() = scope.dependencies

    /**
     * 尝试打开一个随机的网络端口，返回是否成功
     * 若当前已有打开的网络端口则不进行任何操作
     */
    fun openOneNetwork() =
        sockets.view.isNotEmpty()
            || networks.view.keys.firstOrNull()?.also { sockets[it] } != null

    /** 打开所有网络端口，返回实际打开的网络端口数量 */
    fun openAllNetworks() = networks.view.keys.onEach { sockets[it] }.size

    /** 查看超时时间 [timeout] 内出现的组成员 */
    operator fun get(timeout: Int) = group[timeout]

    /** 查看远端 [name] 的地址和端口 */
    operator fun get(name: String) = addresses[name]

    // function

    /** 请求自证存在性 */
    fun yell() = monitor.yell()

    /** 主动询问一个远端的端口 */
    infix fun ask(name: String) = synchronizer2 ask name

    /** 使用指令 [cmd] 广播数据包 [payload] */
    fun broadcast(cmd: Command, payload: ByteArray) = broadcaster.broadcast(cmd, payload)

    /** 使用指令 [cmd] 连接到一个远端 [name] */
    fun connect(name: String, cmd: Command) = client.connect(name, cmd)

    // service

    /** 阻塞等待 UDP 报文 */
    operator fun invoke() = receiver()

    /** 阻塞等待 TCP 连接 */
    fun accept() = server()

    private companion object {
        val ADDRESS = InetSocketAddress(InetAddress.getByName("233.33.33.33"), 23333)
        fun randomName() = "RemoteHub[${UUID.randomUUID()}]"
    }
}
