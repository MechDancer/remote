package org.mechdancer.remote

import org.mechdancer.framework.dependency.must
import org.mechdancer.framework.dependency.plusAssign
import org.mechdancer.framework.dependency.scope
import org.mechdancer.framework.remote.modules.group.GroupMonitor
import org.mechdancer.framework.remote.modules.multicast.MulticastBroadcaster
import org.mechdancer.framework.remote.modules.multicast.MulticastReceiver
import org.mechdancer.framework.remote.resources.Group
import org.mechdancer.framework.remote.resources.MulticastSockets
import org.mechdancer.framework.remote.resources.Name
import org.mechdancer.framework.remote.resources.Networks
import java.net.InetAddress
import java.net.InetSocketAddress
import kotlin.concurrent.thread

private object TestOut {
    @JvmStatic
    fun main(args: Array<String>) {
        val group = Group()
        val monitor = GroupMonitor(::println)

        val scope = scope {
            this += Name("Kotlin")

            this += group
            this += monitor

            val networks = Networks()
            this += MulticastSockets(ADDRESS).apply { networks.view.keys.forEach { this[it] } }
            this += MulticastBroadcaster()
            this += MulticastReceiver()
        }

        val receiver = scope.dependencies.must<MulticastReceiver>()

        thread {
            while (true) {
                monitor.yell()
                Thread.sleep(500)
                println("members: ${group[550]}")
            }
        }

        while (true) receiver()
    }

    val ADDRESS = InetSocketAddress(
        InetAddress.getByName("233.33.33.33"),
        23333
    )
}
