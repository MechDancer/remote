package org.mechdancer.remote.builder

import org.mechdancer.remote.core.RemoteHub

/**
 * 建造一个广播服务器
 *
 * @param name 进程名
 * @param callbacks 请求回调
 */
fun remoteHub(
	name: String,
	callbacks: RemoteCallbackBuilder.() -> Unit
) = RemoteCallbackBuilder()
	.apply(callbacks)
	.run {
		RemoteHub(
			name,
			netFilter,
			newProcessDetected,
			broadcastReceived,
			remoteProcess
		)
	}
