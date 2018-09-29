import java.net.DatagramPacket
import java.net.InetAddress
import java.net.MulticastSocket
import kotlin.concurrent.thread

/**
 * 广播服务器
 * @param name 进程名字
 */
class BroadcastServer(val name: String, callback: String.() -> Unit) {
    //组播监听
    private val socket = MulticastSocket(port)
    //组成员列表
    private val _group = mutableSetOf<String>()

    /** 组成员列表 */
    val group get() = _group.toSet()

    //发送组播 [是否询问: Boolean][名字: String]
    private fun broadcast(active: Boolean) {
        val pack = ByteArray(name.length + 1)
        pack[0] = if (active) 1 else 0
        name.toByteArray().copyInto(pack, 1)
        socket.send(DatagramPacket(pack, pack.size, address, port))
    }

    init {
        //加入组
        socket.joinGroup(address)
        //入组通告
        broadcast(true)
        //持续监听
        thread {
            val receiver = DatagramPacket(ByteArray(128), 128)
            while (true) {
                //收一包
                socket.receive(receiver)
                //解名字
                val name = String(receiver.data, 1, receiver.length - 1)
                //不是自己发的
                if (name != this.name) {
                    //记录名字
                    if (name !in _group) {
                        _group += name
                        callback(name)
                    }
                    //对主动发送进行回复
                    if (receiver.data[0] > 0)
                        broadcast(false)
                }
            }
        }
    }

    private companion object {
        //端口号
        const val port = 2333
        //组播地址
        private val address = InetAddress.getByName("239.0.0.1")
    }
}
