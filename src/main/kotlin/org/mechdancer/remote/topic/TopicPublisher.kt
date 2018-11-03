package org.mechdancer.remote.topic

import org.mechdancer.remote.core.RemoteHub
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream

class TopicPublisher<T>(
	private val topic: String,
	private val hub: RemoteHub,
	private val encoder: (T) -> ByteArray
) {
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
