package org.mechdancer.remote.topic

import org.mechdancer.remote.core.CallBackPlugin
import org.mechdancer.remote.core.RemoteHub
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.io.ObjectOutputStream

/**
 * 话题解析插件
 */
class PublishPlugin(
	val map: Map<String, SerialTool<*>>
) : CallBackPlugin {
	override val id = 'P'

	override fun invoke(
		host: RemoteHub,
		guest: String,
		payload: ByteArray
	): ByteArray =
		ByteArrayOutputStream()
			.apply { ObjectOutputStream(this).writeObject(map[String(payload)]?.input) }
			.toByteArray()

	fun <T> build(topic: String, data: T) =
		@Suppress("UNCHECKED_CAST")
		(map[topic]?.output as? (T) -> ByteArray)
			?.let {
				ByteArrayOutputStream().apply {
					DataOutputStream(this).writeByte(topic.length)
					write(topic.toByteArray())
					write(it(data))
				}
			}
			?.toByteArray()
			?: throw IllegalArgumentException("topic is not register or type goes run")
}
