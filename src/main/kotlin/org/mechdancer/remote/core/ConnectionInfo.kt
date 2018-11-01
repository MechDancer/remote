package org.mechdancer.remote.core

/**
 * 连接信息
 * @param address 地址
 * @param stamp   最后到达时间
 */
data class ConnectionInfo(
	val address: HubAddress?,
	val stamp: Long
)
