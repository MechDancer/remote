package org.mechdancer.remote

import org.mechdancer.framework.dependency.must
import org.mechdancer.framework.dependency.plusAssign
import org.mechdancer.framework.dependency.scope
import org.mechdancer.framework.remote.functions.GroupMonitor
import org.mechdancer.framework.remote.functions.multicast.MulticastBroadcaster
import org.mechdancer.framework.remote.functions.multicast.MulticastReceiver
import org.mechdancer.framework.remote.resources.Group
import org.mechdancer.framework.remote.resources.MulticastSockets
import org.mechdancer.framework.remote.resources.Name
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.NetworkInterface
import kotlin.concurrent.thread

private object TestOut {
    @JvmStatic
    fun main(args: Array<String>) {
        val scope = scope {
            this += Name("Kotlin")

            this += Group()
            this += GroupMonitor(::println)

            this += MulticastSockets(ADDRESS).also { it.get(localHost) }
            this += MulticastBroadcaster()
            this += MulticastReceiver()
        }

        val monitor = scope.must<GroupMonitor>()
        val receiver = scope.must<MulticastReceiver>()

        thread {
            while (true) {
                Thread.sleep(500)
                monitor.yell()
            }
        }

        while (true) receiver.invoke()
    }

    val ADDRESS = InetSocketAddress(
        InetAddress.getByName("238.88.88.88"),
        23333
    )

    val localHost get() = NetworkInterface.getByInetAddress(InetAddress.getLocalHost())
}