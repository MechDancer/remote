package org.mechdancer.remote

import org.mechdancer.version2.dependency.functions.basic.GroupMonitor
import org.mechdancer.version2.dependency.functions.basic.MulticastBroadcaster
import org.mechdancer.version2.dependency.functions.basic.MulticastReceiver
import org.mechdancer.version2.dependency.resource.basic.Group
import org.mechdancer.version2.dependency.resource.basic.MulticastSockets
import org.mechdancer.version2.must
import org.mechdancer.version2.remoteHub
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.NetworkInterface
import kotlin.concurrent.thread

object TestVersion2 {
    private val ADDRESS = InetSocketAddress(InetAddress.getByName("238.88.88.88"), 23333)

    @JvmStatic
    fun main(args: Array<String>) {
        // 初始化
        val temp = remoteHub("version2") {
            // 组成员管理
            this setup Group()
            this setup GroupMonitor { println("detected $it") }

            // 组播
            val sockets = MulticastSockets(ADDRESS)
            this setup sockets                // 组播套接字管理
            this setup MulticastBroadcaster() // 组播发送
            this setup MulticastReceiver()    // 组播接收

            // 添加默认网卡
            sockets[NetworkInterface.getByInetAddress(InetAddress.getLocalHost())]
        }

        // 启动接收线程
        val receiver = temp.functions.must<MulticastReceiver>()
        thread { while (true) receiver() }
    }
}
