package org.mechdancer.remote.core

import org.mechdancer.remote.readNBytes
import java.io.*

data class PackageIO(
	val type: Byte,
	val name: String,
	val payload: ByteArray
) {
	fun toByteArray(): ByteArray =
		ByteArrayOutputStream().apply {
			DataOutputStream(this).apply {
				writeByte(type.toInt())
				writeByte(name.length)
				writeBytes(name)
			}
			write(payload)
		}.toByteArray()

	override fun equals(other: Any?): Boolean {
		if (this === other) return true
		if (javaClass != other?.javaClass) return false

		other as PackageIO

		if (type != other.type) return false
		if (name != other.name) return false
		if (!payload.contentEquals(other.payload)) return false

		return true
	}

	override fun hashCode(): Int {
		var result = type.toInt()
		result = 31 * result + name.hashCode()
		result = 31 * result + payload.contentHashCode()
		return result
	}
}

fun OutputStream.writePack(
	type: Byte,
	name: String,
	payload: ByteArray
) = PackageIO(type, name, payload)
	.toByteArray()
	.let(this::write)

fun InputStream.readPack() =
	DataInputStream(this).let {
		val type = it.readByte()
		val length = it.readByte().toInt()
		val name = String(it.readNBytes(length))
		val payload = it.readBytes()
		PackageIO(type, name, payload)
	}
