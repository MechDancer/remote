package org.mechdancer.remote.topic

import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.io.ObjectOutputStream

/**
 * 默认编码方案
 */
fun encode(vararg data: Any): ByteArray {
	val buffer = ByteArrayOutputStream()
	for (x in data) {
		when (x) {
			is Byte      -> DataOutputStream(buffer).writeByte(x.toInt())
			is Short     -> DataOutputStream(buffer).writeShort(x.toInt())
			is Int       -> DataOutputStream(buffer).writeInt(x)
			is Long      -> DataOutputStream(buffer).writeLong(x)
			is Float     -> DataOutputStream(buffer).writeFloat(x)
			is Double    -> DataOutputStream(buffer).writeDouble(x)
			is ByteArray -> buffer.write(x)
			is String    -> {
				DataOutputStream(buffer).writeByte(x.length)
				buffer.write(x.toByteArray())
			}
			else         -> ObjectOutputStream(buffer).writeObject(x)
		}
	}
	return buffer.toByteArray()
}
