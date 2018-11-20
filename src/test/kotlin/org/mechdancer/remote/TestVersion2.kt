package org.mechdancer.remote

import org.mechdancer.version2.buildHub
import org.mechdancer.version2.dependency.functions.basic.GroupMonitor
import org.mechdancer.version2.dependency.functions.basic.MulticastBroadcaster
import org.mechdancer.version2.dependency.functions.basic.MulticastReceiver
import org.mechdancer.version2.dependency.resources.basic.Group
import org.mechdancer.version2.dependency.resources.basic.MulticastSockets
import org.mechdancer.version2.dependency.resources.basic.Name
import org.mechdancer.version2.must
import org.mechdancer.version2.plusAssign
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.NetworkInterface
import kotlin.concurrent.thread

object TestVersion2 {
    private val ADDRESS = InetSocketAddress(InetAddress.getByName("238.88.88.88"), 23333)

    @JvmStatic
    fun main(args: Array<String>) {
        // 初始化
        val temp = buildHub {
            // 名字
            this += Name("version2")

            // 组成员管理
            this += Group()
            this += GroupMonitor { println("detected $it") }

            // 组播
            val sockets = MulticastSockets(ADDRESS)
            this += sockets                // 组播套接字管理
            this += MulticastBroadcaster() // 组播发送
            this += MulticastReceiver()    // 组播接收

            // 添加默认网卡
            sockets[NetworkInterface.getByInetAddress(InetAddress.getLocalHost())]
        }

        // 启动接收线程
        val receiver = temp.must<MulticastReceiver>()
        thread { while (true) receiver() }
    }
}
