package org.mechdancer.remote.core

import java.io.*
import java.net.*
import java.util.*
import kotlin.math.abs

/**
 * 广播服务器
 * @param name 进程名
 */
class BroadcastHub(
    val name: String,
    private val netFilter: (NetworkInterface) -> Boolean,
    private val newProcessDetected: String.() -> Unit,
    private val broadcastReceived: BroadcastHub.(String, ByteArray) -> Unit,
    private val remoteProcess: (ByteArray) -> ByteArray
) {
    // 组播监听
    private val socket = MulticastSocket(port)
    // TCP监听
    private val server = newServerSocket()
    // 组成员列表
    private val _group = mutableMapOf<String, Pair<Int, InetAddress>?>()
    // TCP挂起锁
    private val tcpLock = Object()

    /** 组成员列表 */
    val group get() = _group.keys

    // 发送组播报文
    private fun send(cmd: Cmd, payload: ByteArray = ByteArray(0)) {
        val header = name.toByteArray()
        val pack = ByteArray(header.size + payload.size + 2)
        pack[0] = cmd.id
        pack[1] = name.length.toByte()
        System.arraycopy(header, 0, pack, 2, header.size)
        System.arraycopy(payload, 0, pack, 2 + name.length, payload.size)
        socket.send(
            DatagramPacket(
                pack, pack.size,
                address,
                port
            )
        )
    }

    // 广播自己的名字
    private fun yell(active: Boolean) =
        if (active) send(Cmd.YellActive) else send(Cmd.YellReply)

    // 广播自己的IP地址
    private fun tcpAck() =
        ByteArrayOutputStream()
            .apply {
                DataOutputStream(this).use {
                    server
                        .localPort
                        .let(it::writeShort)
                    socket
                        .networkInterface
                        .inetAddresses
                        .toList()
                        .first()
                        .address
                        .let(it::write)
                }
            }
            .toByteArray()
            .let { send(Cmd.TcpAck, it) }

    /**
     * 远程调用
     * 通过TCP传输，并阻塞接收反馈
     */
    tailrec fun remoteCall(name: String, msg: ByteArray): ByteArray =
        _group[name]
            ?.let {
                try {
                    Socket(it.second, it.first)
                } catch (e: IOException) {
                    _group[name] = null
                    null
                }
            }
            ?.run {
                getOutputStream().write(msg)
                shutdownOutput()
                getInputStream()
                    .readNBytes(Int.MAX_VALUE)
                    .also { close() }
            }
            ?: run {
                send(Cmd.TcpAsk, name.toByteArray())
                synchronized(tcpLock) { tcpLock.wait() }
                null
            }
            ?: remoteCall(name, msg)

    /**
     * 广播一包数据
     */
    infix fun broadcast(msg: ByteArray) = send(Cmd.Broadcast, msg)

    /**
     * 监听并解析 UDP 包
     */
    operator fun invoke(bufferSize: Int = 2048) {
        val receiver = DatagramPacket(ByteArray(bufferSize), bufferSize)
        var senderName: String
        do {
            //收一包
            socket.receive(receiver)
            //解析名字
            senderName = String(receiver.data, 2, receiver.data[1].toInt())
            //是自己发的则再收一包
            if (senderName == name) continue
            //记录不认识的名字
            if (senderName !in _group) {
                _group[senderName] = null
                newProcessDetected(senderName)
            }
            break
        } while (true)
        //解析负载
        val payload = receiver.data.copyOfRange(senderName.length + 2, receiver.length)
        //响应指令
        when (receiver.data[0].toCmd()) {
            Cmd.YellActive -> yell(active = false)
            Cmd.YellReply -> Unit
            Cmd.TcpAsk -> if (name == String(payload)) tcpAck()
            Cmd.TcpAck -> {
                ByteArrayInputStream(payload)
                    .let(::DataInputStream)
                    .use { it.readShort().toUInt() to it.readNBytes(Int.MAX_VALUE).let(InetAddress::getByAddress) }
                    .also { _group[senderName] = it }
                synchronized(tcpLock) { tcpLock.notifyAll() }
            }
            Cmd.Broadcast -> broadcastReceived(senderName, payload)
            null -> Unit
        }
    }

    /**
     * 监听并解析 TCP 包
     */
    fun listen() {
        server
            .accept()
            .run {
                val ack = getInputStream()
                    .readNBytes(Int.MAX_VALUE)
                    .let(remoteProcess)
                shutdownInput()
                getOutputStream().write(ack)
                close()
            }
    }

    init {
        //选择一条靠谱的网络
        socket.networkInterface =
                NetworkInterface
                    .getNetworkInterfaces()
                    .asSequence()
                    .filter { it.isUp }
                    .filter { it.supportsMulticast() }
                    .filter { !it.isVirtual }
                    .filter(netFilter)
                    .run {
                        firstOrNull(::wlan)
                            ?: firstOrNull(::eth)
                            ?: firstOrNull { !it.isLoopback }
                            ?: first()
                    }
        //加入组播
        socket.joinGroup(address)
        //入组通告
        yell(active = true)
    }

    enum class Cmd(val id: Byte) {
        YellActive(0),
        YellReply(1),
        TcpAsk(2),
        TcpAck(3),
        Broadcast(127)
    }

    private companion object {
        //端口号
        const val port = 23333

        //组播地址
        val address: InetAddress = InetAddress.getByName("238.88.88.88")

        //反映射
        @JvmStatic
        fun Byte.toCmd() =
            Cmd.values().firstOrNull { it.id == this }

        @JvmStatic
        fun wlan(net: NetworkInterface) = net.name.startsWith("wlan")

        @JvmStatic
        fun eth(net: NetworkInterface) = net.name.startsWith("eth")

        @JvmStatic
        fun Short.toUInt() = toInt().let { if (it < 0) it + 65536 else it }

        @JvmStatic
        tailrec fun newServerSocket(): ServerSocket =
            Random()
                .nextInt(65536)
                .let {
                    try {
                        ServerSocket(abs(it))
                    } catch (e: BindException) {
                        null
                    }
                }
                ?: newServerSocket()
    }
}
