package org.mechdancer.remote.topic

import org.mechdancer.remote.core.RemoteHub
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream

/**
 * 话题发布器
 *
 * @param T 话题数据类型
 *
 * @param topic   话题名
 * @param hub     用于发布话题的终端
 * @param encoder 编码器
 */
class TopicPublisher<T>(
	private val topic: String,
	private val hub: RemoteHub,
	private val encoder: (T) -> ByteArray
) {
	/**
	 * 通过话题发布数据
	 */
	infix fun publish(data: T) =
		hub.broadcast(
			'T',
			ByteArrayOutputStream().apply {
				DataOutputStream(this).writeByte(topic.length)
				write(topic.toByteArray())
				write(encoder(data))
			}.toByteArray()
		)
}
