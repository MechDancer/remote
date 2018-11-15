package org.mechdancer.remote.core

import org.mechdancer.remote.core.RemoteHub.TcpCmd.Call
import org.mechdancer.remote.core.RemoteHub.UdpCmd.*
import org.mechdancer.remote.core.plugin.RemotePlugin
import org.mechdancer.remote.core.protocol.*
import java.io.Closeable
import java.net.*
import java.net.InetAddress.getByName
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.max

/**
 * 远程终端
 * 不承担资源管理或调度功能
 * 当前仅支持 IPV4 上的 UDP 组播及 TCP 单播
 *
 * * 1. UDP 组播
 * * 2. TCP 可靠传输
 * * 3. 在线成员嗅探
 * * 4. 插件服务
 *
 * 初始化参数
 *
 * @param name       进程名
 * @param network    组播使用的（去往外网的）网络接口
 * @param memberMap  已知组成员集，专用于复制终端修改常量
 * @param addressMap 已知 TCP 地址集，专用于复制终端修改常量
 *
 * 回调参数
 * @param newMemberDetected 发现新成员
 * @param broadcastReceived 收到广播
 * @param commandReceived   收到通用 TCP
 */
@Suppress("UNCHECKED_CAST")
class RemoteHub(
    name: String?,
    network: NetworkInterface,

    memberMap: MemberMap?,
    addressMap: AddressMap?,

    private val newMemberDetected: String.() -> Unit,
    private val broadcastReceived: Received,
    private val commandReceived: CallBack

) : Closeable {

    // 默认套接字
    private val default: MulticastSocket
    // TCP监听
    private val server = ServerSocket(0)
    // 组成员列表
    private val addressManager = AddressManager(addressMap)
    // 地址询问阻塞
    private val addressSignal = SignalBlocker()

    // 组成员列表
    private val groupManager = GroupManager(memberMap)
    // 存活时间
    private val aliveTime = AtomicInteger(10000)

    //插件服务
    private val plugins = mutableMapOf<RemotePlugin.Key<*>, RemotePlugin>()

    /**
     * 终端名字
     */
    val name: String

    /**
     * 终端地址
     */
    val address: InetSocketAddress

    /**
     * 组成员列表
     */
    val members: Map<String, InetSocketAddress?>
        get() {
            val livingMembers = groupManager filterByTime timeToLive.toLong()
            return addressManager.filterKeys { it in livingMembers }
        }

    /**
     * 存活时间条件
     */
    var timeToLive: Int
        get() = aliveTime.get()
        set(value) = aliveTime.set(if (value <= 0) Int.MAX_VALUE else value)

    // 更新成员信息
    private fun updateGroup(sender: String) =
        sender.takeIf(String::isNotBlank)
            ?.takeIf(groupManager::detect)
            ?.let(newMemberDetected)

    // 发送组播报文
    private fun broadcast(cmd: Byte, payload: ByteArray = ByteArray(0)) =
        RemotePackage(cmd, name, payload)
            .bytes
            .let { DatagramPacket(it, it.size, ADDRESS) }
            .let(default::send)

    // 广播自己的IP地址
    private fun tcpAck() = broadcast(AddressAck.id, address.bytes)

    // 解析收到的IP地址
    private fun tcpParse(sender: String, payload: ByteArray) {
        if (sender.isBlank()) return
        addressManager[sender] = inetSocketAddress(payload)
        addressSignal.awake()
    }

    /**
     * 广播自己的名字
     * 使得所有在线节点也广播自己的名字，从而得知完整的组列表。
     */
    fun yell() = broadcast(YellActive.id)

    /**
     * 广播一包数据
     *
     * @param msg 数据报
     */
    infix fun broadcast(msg: ByteArray) = broadcast(Broadcast.id, msg)

    /**
     * 广播一包数据
     * 用于插件服务
     *
     * @param id  插件识别号
     * @param msg 数据报
     */
    fun broadcast(id: Char, msg: ByteArray) = broadcast(id.toByte(), msg)

    // 处理 UDP 包
    private fun processUdp(pack: ByteArray) =
        RemotePackage(pack)
            .takeIf { it.sender != name }
            ?.also { (id, sender, payload) ->
                // 更新时间
                updateGroup(sender)
                // 响应指令
                when (UdpCmd(id)) {
                    UdpCmd.YellActive -> broadcast(YellReply.id)
                    UdpCmd.YellReply  -> Unit
                    UdpCmd.AddressAsk -> if (name == String(payload)) tcpAck() else Unit
                    UdpCmd.AddressAck -> tcpParse(sender, payload)
                    UdpCmd.Broadcast  -> broadcastReceived(sender, payload)
                    null              ->
                        id.toChar()
                            .takeIf(Char::isLetterOrDigit)
                            ?.let { pluginId ->
                                plugins[plugins.keys.find { it.id == pluginId }]
                            }
                            ?.invoke(this@RemoteHub, sender, payload)
                }
            }

    /**
     * 更新成员表
     *
     * @param timeout 以毫秒为单位的检查时间，方法最多阻塞这么长时间。
     *                此时间会覆盖之前设置的离线时间。
     */
    infix fun refresh(timeout: Int): Set<String> {
        assert(timeout > 0)

        yell()
        multicastOn(null).use {
            udpReceiveLoop(it, 256, timeout) { pack ->
                val (_, sender, _) = RemotePackage(pack)
                if (sender != name) updateGroup(sender)
            }
        }
        timeToLive = max(timeout + 20, 200)
        return groupManager filterByTime timeToLive.toLong()
    }

    /**
     * 监听并解析 UDP 包
     *
     * @param timeout    以毫秒为单位的超时时间，方法最多阻塞这么长时间。
     *                   默认值为 0，指示超时时间无穷大。
     * @param bufferSize 缓冲区大小，超过缓冲容量的数据包无法接收。
     *                   默认值 65536 是 UDP 支持的最大包长度。
     */
    operator fun invoke(
        timeout: Int = 0,
        bufferSize: Int = 65536
    ) {
        assert(timeout > 0)
        assert(bufferSize in 0..65536)
        if (timeout == 0)
            DatagramPacket(ByteArray(bufferSize), bufferSize)
                .apply(default::receive)
                .actualData
                .let(::processUdp)
        else
            multicastOn(null).use {
                udpReceiveLoop(it, bufferSize, timeout, ::processUdp)
            }
    }

    // 尝试连接一个远端 TCP 服务器
    private fun connect(other: String): Socket {
        while (true) {
            (addressManager connect other)?.also { return it }
            broadcast(AddressAsk.id, other.toByteArray())
            addressSignal.block(1000)
        }
    }

    /**
     * 通过 TCP 发送，并在传输完后立即返回
     *
     * @param other 目标终端名字
     * @param msg   报文
     */
    fun send(other: String, msg: ByteArray) =
        connect(other)
            .getOutputStream()
            .writeWithLength(RemotePackage(Call.id, name, msg).bytes)
            .close()

    // 通用远程调用
    private fun Socket.call(id: Byte, msg: ByteArray) =
        use {
            it.getOutputStream().writeWithLength(RemotePackage(id, name, msg).bytes)
            it.getInputStream().readWithLength()
        }

    /**
     * 通过 TCP 发送，并阻塞接收反馈
     *
     * @param other 目标终端名字
     * @param msg   报文
     */
    fun call(other: String, msg: ByteArray) =
        connect(other).call(TcpCmd.CallBack.id, msg)

    /**
     * 调用 TCP 插件服务
     */
    fun call(id: Char, other: String, msg: ByteArray) =
        connect(other).call(id.toByte(), msg)

    /**
     * 监听并解析 TCP 包
     */
    fun listen() =
        server.accept()
            .use { server ->
                val (id, sender, payload) = RemotePackage(server.getInputStream().readWithLength())
                updateGroup(sender)
                fun reply(msg: ByteArray) = server.getOutputStream().writeWithLength(msg)
                when (TcpCmd(id)) {
                    TcpCmd.Call     -> commandReceived(sender, payload)
                    TcpCmd.CallBack -> commandReceived(sender, payload).let(::reply)
                    null            ->
                        id.toChar()
                            .takeIf(Char::isLetterOrDigit)
                            ?.let { pluginId ->
                                plugins[plugins.keys.find { it.id == pluginId }]
                            }
                            ?.onCall(this, sender, payload)
                            ?.let(::reply)
                            ?: Unit
                }
            }

    /**
     * 加载插件
     */
    infix fun setup(plugin: RemotePlugin) {
        assert(plugin.key.id.isLetterOrDigit())
        plugins[plugin.key] = plugin
        plugin.onSetup(this)
    }

    /**
     * 卸载插件
     */
    infix fun <T : RemotePlugin> teardown(key: RemotePlugin.Key<T>): T? =
        this[key]?.let {
            it.onTeardown()
            plugins.remove(key) as? T
        }

    operator fun <T : RemotePlugin> get(key: RemotePlugin.Key<T>) = plugins[key]

    /**
     * 停止所有功能，释放资源
     * 调用此方法后再用终端进行收发操作将导致异常、阻塞或其他非预期的结果。
     */
    override fun close() {
        plugins.forEach { _, plugin -> plugin.onTeardown() }
        plugins.clear()
        default.leaveGroup(ADDRESS.address)
        default.close()
        server.close()
    }

    init {
        address = InetSocketAddress(
            network.inetAddresses.asSequence().first(::isHost),
            server.localPort
        )
        // 定名
        this.name = name ?: "Hub[$address]"
        // 入组
        default = multicastOn(network)
    }

    // 指令 ID
    private enum class UdpCmd(val id: Byte) {
        YellActive(0),
        YellReply(1),
        AddressAsk(2),
        AddressAck(3),
        Broadcast(127);

        companion object {
            operator fun invoke(byte: Byte) =
                values().firstOrNull { it.id == byte }
        }
    }

    // 指令 ID
    private enum class TcpCmd(val id: Byte) {
        Call(0),
        CallBack(1);

        companion object {
            operator fun invoke(byte: Byte) =
                values().firstOrNull { it.id == byte }
        }
    }

    private companion object {
        val ADDRESS = InetSocketAddress(getByName("238.88.88.88"), 23333)

        fun multicastOn(net: NetworkInterface?) =
            MulticastSocket(ADDRESS.port).apply {
                net?.let(this::setNetworkInterface)
                joinGroup(ADDRESS.address)
            }

        fun isHost(address: InetAddress) =
            address
                .let { it as? Inet4Address }
                ?.address
                ?.first()
                ?.let { it + if (it > 0) 0 else 256 }
                ?.takeIf { it in 1..223 } != null

        // 拆解 UDP 数据包
        val DatagramPacket.actualData
            get() = data.copyOfRange(0, length)

        // UDP 接收循环
        fun udpReceiveLoop(
            socket: DatagramSocket,
            bufferSize: Int,
            timeout: Int,
            block: (ByteArray) -> Any?
        ) {
            val buffer = DatagramPacket(ByteArray(bufferSize), bufferSize)
            val endTime = System.currentTimeMillis() + timeout
            while (true) {
                // 设置超时时间
                socket.soTimeout =
                    (endTime - System.currentTimeMillis())
                        .toInt()
                        .takeIf { it > 10 }
                    ?: return
                // 接收，超时直接退出
                try {
                    socket.receive(buffer)
                } catch (_: SocketTimeoutException) {
                    return
                }
                block(buffer.actualData)
            }
        }
    }
}
