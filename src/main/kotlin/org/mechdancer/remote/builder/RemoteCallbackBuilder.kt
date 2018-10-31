package org.mechdancer.remote.builder

import org.mechdancer.remote.core.RemoteHub
import java.net.NetworkInterface
import java.rmi.Remote

/**
 * 消息回调 Dsl 建造者
 */
class RemoteCallbackBuilder<T : Remote> {
	var netFilter: (NetworkInterface) -> Boolean = { true }
	var newMemberDetected: String.() -> Unit = {}
	var broadcastReceived: RemoteHub<T>.(String, ByteArray) -> Unit = { _, _ -> }
	var commandReceived: RemoteHub<T>.(String, ByteArray) -> ByteArray = { _, _ -> ByteArray(0) }
	var rmiRemote: T? = null
}
