package org.mechdancer.remote.core

import java.net.DatagramPacket
import java.net.InetAddress
import java.net.MulticastSocket
import java.net.NetworkInterface

/**
 * 广播服务器
 * @param name 进程名字
 */
class BroadcastHub(
    val name: String,
    private val newProcessDetected: String.() -> Unit,
    private val broadcastReceived: BroadcastHub.(String, ByteArray) -> Unit
) {
    //组播监听
    private val socket = MulticastSocket(port)
    //组成员列表
    private val _group = mutableSetOf<String>()

    /** 组成员列表 */
    val group get() = _group.toSet()

    //发送组播报文
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

    //广播自己的名字
    private fun yell(active: Boolean) =
        if (active) send(Cmd.YellActive) else send(Cmd.YellReply)

    /** 广播一包数据 */
    infix fun broadcast(msg: ByteArray) = send(Cmd.Broadcast, msg)

    /**
     * 接收并解析数据包
     */
    operator fun invoke(bufferSize: Int = 2048) {
        val receiver = DatagramPacket(ByteArray(bufferSize), bufferSize)
        while (true) {
            //收一包
            socket.receive(receiver)
            //解析名字
            val name = String(receiver.data, 2, receiver.data[1].toInt())
            //是自己发的则再收一包
            if (name == this.name) continue
            //记录不认识的名字
            if (name !in _group) {
                _group += name
                newProcessDetected(name)
            }
            break
        }
        //解析负载
        val payload = receiver.data.copyOfRange(name.length + 2, receiver.length)
        //响应指令
        when (receiver.data[0].toCmd()) {
            Cmd.YellActive -> yell(active = false)
            Cmd.YellReply -> Unit
            Cmd.Broadcast -> broadcastReceived(name, payload)
            null -> Unit
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
                    .run {
                        firstOrNull(::wlan)
                            ?: firstOrNull(::eth)
                            ?: firstOrNull { !it.isLoopback }
                            ?: firstOrNull()
                    }
        //加入组播
        socket.joinGroup(address)
        //入组通告
        yell(active = true)
    }

    enum class Cmd(val id: Byte) {
        YellActive(0),
        YellReply(1),
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
    }
}
