@file:Suppress("UNCHECKED_CAST")

package org.mechdancer.remote.topic

import org.mechdancer.remote.core.RemoteHub
import org.mechdancer.remote.core.load
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.rmi.RemoteException
import java.rmi.server.UnicastRemoteObject

/**
 * 数据发布服务器
 * @param core 挂载服务的终端
 * @param map  话题编解码方案
 */
class PublishServer(
	val core: RemoteHub,
	val map: Map<String, SerialTool<*>>
) {
	init {
		core.load<ParserServer>(object : UnicastRemoteObject(), ParserServer {
			override operator fun <T> get(topic: String) =
				@Suppress("UNCHECKED_CAST")
				map[topic]
					?.let { it as? SerialTool<T> }
					?.input
					?: throw RemoteException("topic not exist or type goes wrong")
		})
	}

	/**
	 * 发送数据
	 * @param topic 话题名
	 * @param data  数据
	 */
	operator fun <T> set(topic: String, data: T) {
		val pack = map[topic]?.output as? (T) -> ByteArray
		if (pack != null) {
			ByteArrayOutputStream().apply {
				DataOutputStream(this).writeByte(topic.length)
				this.write(topic.toByteArray())
				this.write(pack(data))
			}.toByteArray().let(core::broadcast)
		} else throw IllegalArgumentException("topic is not register")
	}
}
