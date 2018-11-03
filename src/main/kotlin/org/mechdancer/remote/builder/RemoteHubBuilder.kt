package org.mechdancer.remote.builder

import org.mechdancer.remote.core.RemoteHub

/**
 * 建造一个广播服务器
 *
 * @param name  进程名
 * @param block 请求回调
 */
fun remoteHub(
	name: String = "",
	block: RemoteCallbackBuilder.() -> Unit = {}
) = RemoteCallbackBuilder()
	.apply(block)
	.let { info ->
		RemoteHub(
			name,
			info.netFilter,
			info.newMemberDetected,
			info.broadcastReceived,
			info.commandReceived
		).also { hub ->
			info.plugins.forEach(hub::setup)
		}
	}
