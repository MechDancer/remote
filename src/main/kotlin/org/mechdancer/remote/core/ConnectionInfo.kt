package org.mechdancer.remote.core

import java.net.InetSocketAddress

/**
 * 连接信息
 * @param address 地址
 * @param stamp   最后到达时间
 */
data class ConnectionInfo(
	val address: InetSocketAddress?,
	val stamp: Long
)
