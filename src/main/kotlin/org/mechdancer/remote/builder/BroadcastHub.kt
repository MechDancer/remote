package org.mechdancer.remote.builder

import org.mechdancer.remote.core.RemoteHub
import java.rmi.Remote

/**
 * 建造一个广播服务器
 *
 * @param name  进程名
 * @param block 请求回调
 */
fun <T : Remote> remoteHub(
	name: String = "",
	block: RemoteCallbackBuilder<T>.() -> Unit
) = RemoteCallbackBuilder<T>()
	.apply(block)
	.run {
		RemoteHub(
			name,
			netFilter,
			newProcessDetected,
			broadcastReceived,
			remoteProcess,
			rmiRemote
		)
	}
