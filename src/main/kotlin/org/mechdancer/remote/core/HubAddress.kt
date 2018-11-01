package org.mechdancer.remote.core

import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.net.InetAddress

/**
 * 终端地址
 *
 * @param address IP  地址
 * @param tcpPort TCP 监听端口号
 * @param rmiPort RMI 服务端口号
 */
class HubAddress(
	val address: InetAddress,
	val tcpPort: Int,
	val rmiPort: Int
) {
	val bytes: ByteArray =
		ByteArrayOutputStream().apply {
			write(address.address)
			DataOutputStream(this).apply {
				writeInt(tcpPort)
				writeInt(rmiPort)
			}
		}.toByteArray()

	override fun toString() =
		"${address.hostAddress}:$tcpPort|$rmiPort"
}
