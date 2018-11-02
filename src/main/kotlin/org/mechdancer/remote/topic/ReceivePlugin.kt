package org.mechdancer.remote.topic

import org.mechdancer.remote.core.BroadcastPlugin
import org.mechdancer.remote.core.RemoteHub
import org.mechdancer.remote.core.connectRMI
import kotlin.concurrent.thread

/**
 * 话题接收插件
 */
class ReceivePlugin(
	val callback: (String, String, Any?) -> Unit
) : BroadcastPlugin {
	private val topics = mutableMapOf<String, (ByteArray) -> Any?>()

	override val id = 'T'
	override operator fun invoke(host: RemoteHub, guest: String, payload: ByteArray) {
		var flag = true
		thread { while (flag) host() }
		val topic = String(payload.copyOfRange(1, 1 + payload[0]))
		val data = payload.copyOfRange(1 + payload[0], payload.size)
		val result = topics[topic]
			?.invoke(data)
			?: run {
				val f = host.connectRMI<ParserServer>(guest)?.get(topic)
				if (f != null) topics[topic] = f
				else throw RuntimeException("cannot parse topic")
				f(data)
			}
		callback(guest, topic, result)
		flag = false
	}
}
