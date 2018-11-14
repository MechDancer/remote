package org.mechdancer.remote.core

import java.net.InetSocketAddress
import java.net.Socket
import java.net.SocketException

/**
 * 地址管理器
 */
class AddressManager
	: MutableMap<String, InetSocketAddress?> by mutableMapOf() {
	/**
	 * 尝试连接到一个远端
	 */
	infix fun connect(name: String) =
		get(name)?.let { ip ->
			val socket = Socket()
			socket.soTimeout = 100
			try {
				socket.connect(ip)
				socket
			} catch (e: SocketException) {
				set(name, null)
				socket.close()
				null
			}
		}
}
