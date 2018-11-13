package org.mechdancer.remote.core.protocol

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.net.InetAddress
import java.net.InetSocketAddress

private const val LENGTH = 4 + Int.SIZE_BYTES

/**
 * 从字节数组恢复完整地址
 */
fun inetSocketAddress(byteArray: ByteArray) =
	byteArray
		.takeIf { it.size == LENGTH }
		?.let(::ByteArrayInputStream)
		?.let(::DataInputStream)
		?.use {
			InetSocketAddress(
				InetAddress.getByAddress(it.waitNBytes(4)),
				it.readInt()
			)
		}

/**
 * 地址打包到字节数组
 */
val InetSocketAddress.bytes: ByteArray
	get() =
		ByteArrayOutputStream().apply {
			write(address.address)
			DataOutputStream(this).writeInt(port)
		}.toByteArray()
