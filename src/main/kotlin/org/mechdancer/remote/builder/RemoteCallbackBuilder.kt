package org.mechdancer.remote.builder

import org.mechdancer.remote.core.RemotePlugin
import org.mechdancer.remote.core.internal.CallBack
import org.mechdancer.remote.core.internal.Received
import org.mechdancer.remote.network.MULTICAST_FILTERS
import org.mechdancer.remote.network.NetFilter
import org.mechdancer.remote.network.WIRELESS_FIRST
import java.net.NetworkInterface

/**
 * 消息回调 Dsl 建造者
 */
class RemoteCallbackBuilder internal constructor() {
    var filters1: Collection<NetFilter> = MULTICAST_FILTERS
    var filters2: Collection<NetFilter> = WIRELESS_FIRST
    var selector: (Collection<NetworkInterface>) -> NetworkInterface? = Collection<NetworkInterface>::firstOrNull

    var newMemberDetected: String.() -> Unit = {}
    var broadcastReceived: Received = { _, _ -> }
    var commandReceived: CallBack = { _, _ -> ByteArray(0) }

    inner class Plugins internal constructor() {
        internal val cache = mutableSetOf<RemotePlugin>()

        infix fun setup(plugin: RemotePlugin) = cache.add(plugin)
    }

    val plugins = Plugins()

    fun plugins(block: Plugins.() -> Unit) = plugins.apply(block)
}
