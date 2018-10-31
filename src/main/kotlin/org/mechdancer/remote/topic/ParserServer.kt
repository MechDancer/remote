package org.mechdancer.remote.topic

import java.rmi.Remote
import java.rmi.RemoteException

/**
 * 根据话题名，远程获取解析器
 */
interface ParserServer : Remote {
	@Throws(RemoteException::class)
	operator fun <T> get(topic: String): (ByteArray) -> T
}
