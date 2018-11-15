package org.mechdancer.remote.core.network

import org.mechdancer.remote.builder.remoteHub
import java.net.NetworkInterface

typealias NetFilter = (NetworkInterface) -> Boolean

/**
 * 选网函数
 * @param filters1    从宽到严的规则集
 * @param filters2    从严到宽的规则集
 * @param filterFinal 终选条件
 * @return 最终选到的网络
 */
fun selectNetwork(
	filters1: Collection<NetFilter>,
	filters2: Collection<NetFilter>,
	filterFinal: NetFilter
): NetworkInterface? =
	NetworkInterface
		.getNetworkInterfaces()
		.toList()
		.filter(NetworkInterface::isUp)
		.tryFilter(filters1)
		.tryBest(filters2)
		.takeIf(Collection<*>::isNotEmpty)
		?.waitSingle(filterFinal)

/**
 * 检查网络中是否包含特定成员
 * @param name 目标成员名字
 */
operator fun NetworkInterface.contains(name: String) =
	name in remoteHub("", this) refresh 600
