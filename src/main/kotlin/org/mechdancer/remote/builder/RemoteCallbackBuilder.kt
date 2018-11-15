package org.mechdancer.remote.builder

import org.mechdancer.remote.core.RemoteHub
import org.mechdancer.remote.core.network.MULTICAST_FILTERS
import org.mechdancer.remote.core.network.NetFilter
import org.mechdancer.remote.core.network.WIRELESS_FIRST
import org.mechdancer.remote.core.plugin.RemotePlugin
import java.net.NetworkInterface

/**
 * 消息回调 Dsl 建造者
 */
class RemoteCallbackBuilder(
	var network: NetworkInterface? = null,
	var filters1: Collection<NetFilter> = MULTICAST_FILTERS,
	var filters2: Collection<NetFilter> = WIRELESS_FIRST,
	var filterFinal: NetFilter = { true },
	var newMemberDetected: String.() -> Unit = {},
	var broadcastReceived: RemoteHub.(String, ByteArray) -> Unit = { _, _ -> },
	var commandReceived: RemoteHub.(String, ByteArray) -> ByteArray = { _, _ -> ByteArray(0) }
) {
	inner class Plugins {
		infix fun setup(plugin: RemotePlugin) = plugins.add(plugin)
	}

	internal val plugins = mutableSetOf<RemotePlugin>()

	fun plugins(block: Plugins.() -> Unit) =
		Plugins().apply(block)
}
