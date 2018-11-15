package org.mechdancer.remote.builder

import org.mechdancer.remote.core.RemoteHub
import org.mechdancer.remote.core.network.selectNetwork
import org.mechdancer.remote.core.plugin.RemotePlugin
import java.net.NetworkInterface

/**
 * 建造一个广播服务器
 *
 * @param name  进程名
 * @param block 请求回调
 */
fun remoteHub(
	name: String? = null,
	network: NetworkInterface? = null,
	block: RemoteCallbackBuilder.() -> Unit = {}
) = RemoteCallbackBuilder()
	.apply(block)
	.let { info ->
		RemoteHub(
			name,
			network
				?: selectNetwork(
					info.filters1,
					info.filters2,
					info.filterFinal
				)
				?: throw RuntimeException("no available network"),

			null,
			null,

			info.newMemberDetected,
			info.broadcastReceived,
			info.commandReceived
		).also { hub ->
			info.plugins.forEach { hub.setup<RemotePlugin>(it) }
		}
	}
