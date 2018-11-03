package org.mechdancer.remote.topic

import org.mechdancer.remote.core.CallBackPlugin
import org.mechdancer.remote.core.RemoteHub
import java.io.ByteArrayOutputStream
import java.io.ObjectOutputStream

/**
 * 话题解析插件
 */
class ParserPlugin(
	map: Map<String, (ByteArray) -> Any?>,
	vararg topics: String
) : CallBackPlugin {
	private val functions =
		if (topics.isNotEmpty()) map.filterKeys { it in topics } else map

	override val id = 'P'

	override fun invoke(
		host: RemoteHub,
		guest: String,
		payload: ByteArray
	): ByteArray =
		ByteArrayOutputStream()
			.apply { ObjectOutputStream(this).writeObject(functions[String(payload)]) }
			.toByteArray()
}
