package org.mechdancer.remote

import org.mechdancer.version2.RemoteHub
import org.mechdancer.version2.dependency.functions.basic.GroupMonitor
import org.mechdancer.version2.dependency.functions.basic.MulticastBroadcaster
import org.mechdancer.version2.dependency.functions.basic.MulticastReceiver
import org.mechdancer.version2.dependency.resource.basic.Group
import org.mechdancer.version2.dependency.resource.basic.MulticastSockets
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
        val temp = RemoteHub("version2")  // 初始化

        temp += Group()                   // 组成员管理（资源）
        temp += GroupMonitor {
            println("detected $it")       // 组成员管理（功能）
        }

        temp += MulticastSockets(ADDRESS) // 组播套接字管理
        temp += MulticastBroadcaster()    // 组播发送
        temp += MulticastReceiver()       // 组播小包接收

        temp.syncDependencies()           // 扫描依赖项

        temp.resources.must<MulticastSockets>()[
            NetworkInterface.getByInetAddress(InetAddress.getLocalHost())
        ]

        // 启动接收线程
        val receiver = temp.functions.must<MulticastReceiver>()
        thread { while (true) receiver() }
    }
}
