package org.mechdancer.remote.builder

import org.mechdancer.remote.core.RemoteHub
import java.net.NetworkInterface
import java.rmi.Remote

/**
 * 消息回调 Dsl 建造者
 */
class RemoteCallbackBuilder(
	var netFilter: (NetworkInterface) -> Boolean = { true },
	var newMemberDetected: String.() -> Unit = {},
	var broadcastReceived: RemoteHub.(String, ByteArray) -> Unit = { _, _ -> },
	var commandReceived: RemoteHub.(String, ByteArray) -> ByteArray = { _, _ -> ByteArray(0) }
) {
	val services = mutableMapOf<String, Remote>()
}
