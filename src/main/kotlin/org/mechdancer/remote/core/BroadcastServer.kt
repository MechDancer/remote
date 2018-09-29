package org.mechdancer.remote.core

import java.net.DatagramPacket
import java.net.InetAddress
import java.net.MulticastSocket
import kotlin.concurrent.thread

/**
 * 广播服务器
 * @param name 进程名字
 */
class BroadcastServer(
    val name: String,
    private val newProcessDetected: String.() -> Unit,
    private val broadcastReceived: String.(ByteArray) -> Unit
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
        header.copyInto(pack, 2)
        payload.copyInto(pack, 2 + name.length)
        socket.send(
            DatagramPacket(
                pack, pack.size,
                address,
                port
            )
        )
    }

    private fun yell(active: Boolean) =
        if (active) send(Cmd.YellActive) else send(Cmd.YellReply)

    /** 广播一包数据 */
    infix fun broadcast(msg: ByteArray) = send(Cmd.Broadcast, msg)

    init {
        //加入组
        socket.joinGroup(address)
        //入组通告
        yell(active = true)
        //持续监听
        thread {
            val receiver = DatagramPacket(ByteArray(2048), 2048)
            while (true) {
                //收一包
                socket.receive(receiver)
                //判断指令
                val id = receiver.data[0]
                //解析名字
                val name = String(receiver.data, 2, receiver.data[1].toInt())
                //解析负载
                val payload = receiver.data.copyOfRange(name.length + 2, receiver.data.lastIndex)
                //是自己发的
                if (name == this.name) continue
                //记录名字
                if (name !in _group) {
                    _group += name
                    newProcessDetected(name)
                }
                //响应
                when (id.toCmd()) {
                    Cmd.YellActive -> yell(active = false)
                    Cmd.YellReply  -> Unit
                    Cmd.Broadcast  -> broadcastReceived(name, payload)
                    null           -> Unit
                }
            }
        }
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
        val address: InetAddress = InetAddress.getByName("239.0.0.1")

        //反映射
        fun Byte.toCmd() =
            Cmd.values().firstOrNull { it.id == this }
    }
}
