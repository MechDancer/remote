package org.mechdancer.remote.topic

import org.mechdancer.remote.core.BroadcastPlugin
import org.mechdancer.remote.core.RemoteHub
import org.mechdancer.remote.core.SignalBlocker
import java.io.ByteArrayInputStream
import java.io.ObjectInputStream
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentSkipListSet

/**
 * 话题接收插件
 * 非阻塞，将启动新线程
 * @param callback 接收到话题数据的回调
 */
class ReceivePlugin(
	val callback: (sender: String, topic: String, data: Any?) -> Unit
) : BroadcastPlugin {
	// 话题解析函数的缓存
	private val memory = ConcurrentHashMap<String, (ByteArray) -> Any?>()
	private val queue = ConcurrentSkipListSet<String>()
	private val blocker = SignalBlocker()

	override val id = 'T'

	// 异步等待
	private fun wait(topic: String) =
		CompletableFuture
			.supplyAsync {
				while (topic in queue) blocker.block()
				memory[topic]
			}

	// 异步询问
	private fun RemoteHub.ask(guest: String, topic: String) =
		@Suppress("UNCHECKED_CAST")
		CompletableFuture
			.supplyAsync {
				queue += topic
				call('P', guest, topic.toByteArray())
					.let(::ByteArrayInputStream)
					.let(::ObjectInputStream)
					.readObject()
					.runCatching { this as (ByteArray) -> Any? }
					.getOrNull()
					?.also {
						memory[topic] = it
						queue -= topic
						blocker.awake()
					}
			}

	override operator fun invoke(host: RemoteHub, guest: String, payload: ByteArray) {
		val topic = String(payload.copyOfRange(1, 1 + payload[0]))
		val data = payload.copyOfRange(1 + payload[0], payload.size)
		memory[topic]
			?.let { callback(guest, topic, it(data)) }
			?: (if (topic in queue) wait(topic) else host.ask(guest, topic))
				.thenAccept {
					if (it != null) callback(guest, topic, it(data))
					else throw RuntimeException("cannot parse topic")
				}
	}
}
