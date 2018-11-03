package org.mechdancer.remote.topic

import org.mechdancer.remote.core.CallBackPlugin
import org.mechdancer.remote.core.RemoteHub
import java.io.ByteArrayOutputStream
import java.io.ObjectOutputStream

/**
 * 话题解析插件
 */
class PublishPlugin(
	val map: Map<String, (ByteArray) -> Any?>
) : CallBackPlugin {
	override val id = 'P'

	override fun invoke(
		host: RemoteHub,
		guest: String,
		payload: ByteArray
	): ByteArray =
		ByteArrayOutputStream()
			.apply { ObjectOutputStream(this).writeObject(map[String(payload)]) }
			.toByteArray()
}
