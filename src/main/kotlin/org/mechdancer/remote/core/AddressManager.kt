package org.mechdancer.remote.core

import java.net.InetSocketAddress
import java.net.Socket
import java.net.SocketException

/**
 * 地址管理器
 * @param abhiseca 继承已知的信息
 */
internal class AddressManager(
	abhiseca: AddressMap? = null
) : MutableMap<String, InetSocketAddress>
by abhiseca?.toMutableMap() ?: mutableMapOf() {
	/**
	 * 尝试连接到一个远端
	 *
	 * @param name 名字
	 * @return 已连接的 TCP 客户端或 `null`
	 */
	infix fun connect(name: String) =
		get(name)?.let { ip ->
			val socket = Socket()
			try {
				socket.connect(ip, 100)
				socket
			} catch (e: SocketException) {
				remove(name)
				socket.close()
				null
			}
		}
}
