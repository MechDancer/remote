package org.mechdancer.remote.topic

import org.mechdancer.remote.core.BroadcastPlugin
import org.mechdancer.remote.core.RemoteHub
import java.io.ByteArrayInputStream
import java.io.ObjectInputStream
import java.util.concurrent.CompletableFuture

/**
 * 话题接收插件
 * 非阻塞，将启动 0~1 个新线程
 * @param callback 接收到话题数据的回调
 */
class ReceivePlugin(
	val callback: (sender: String, topic: String, data: Any?) -> Unit
) : BroadcastPlugin {
	// 话题解析函数的缓存
	private val memory = mutableMapOf<String, (ByteArray) -> Any?>()

	override val id = 'T'
	@Suppress("UNCHECKED_CAST")
	override operator fun invoke(host: RemoteHub, guest: String, payload: ByteArray) {
		val topic = String(payload.copyOfRange(1, 1 + payload[0]))
		val data = payload.copyOfRange(1 + payload[0], payload.size)
		memory[topic]
			?.let { callback(guest, topic, it(data)) }
			?: CompletableFuture
				.supplyAsync {
					host.call('P', guest, topic.toByteArray())
						.let(::ByteArrayInputStream)
						.let(::ObjectInputStream)
						.readObject()
						.let { it as? (ByteArray) -> Any? }
						?.also { memory[topic] = it }
						?.invoke(data)
						?: throw RuntimeException("cannot parse topic")
				}
				.thenAccept { callback(guest, topic, it) }
	}
}
