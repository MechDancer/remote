package org.mechdancer.remote

import org.mechdancer.framework.dependency.must
import org.mechdancer.framework.dependency.plusAssign
import org.mechdancer.framework.dependency.scope
import org.mechdancer.framework.remote.functions.GroupMonitor
import org.mechdancer.framework.remote.functions.GroupRefresher
import org.mechdancer.framework.remote.functions.multicast.MulticastBroadcaster
import org.mechdancer.framework.remote.functions.multicast.MulticastReceiver
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
        val scope = scope {
            this += Name("Kotlin")

            this += Group()
            this += GroupMonitor(::println)
            this += GroupRefresher()

            val networks = Networks().apply { scan() }
            this += MulticastSockets(ADDRESS).apply { networks.view.keys.forEach { this[it] } }
            this += MulticastBroadcaster()
            this += MulticastReceiver()
        }

        val group = scope.must<Group>()
        val refresher = scope.must<GroupRefresher>()
        val receiver = scope.must<MulticastReceiver>()

        thread {
            while (true) println("members: ${refresher(500)}")
        }

        while (true) receiver()
    }

    val ADDRESS = InetSocketAddress(
        InetAddress.getByName("238.88.88.88"),
        23333
    )
}