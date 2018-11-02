package org.mechdancer.remote.topic

import org.mechdancer.remote.core.BroadcastPlugin
import org.mechdancer.remote.core.RemoteHub
import org.mechdancer.remote.core.connectRMI
import kotlin.concurrent.thread

class ReceivePlugin : BroadcastPlugin {
	override val id = 'T'
	val topics = mutableMapOf<String, (ByteArray) -> Any?>()
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
		println(result)
		flag = false
	}
}
