package org.mechdancer.remote.util.network

import org.mechdancer.remote.builder.remoteHub
import java.net.NetworkInterface

typealias NetFilter = (NetworkInterface) -> Boolean

/**
 * 选网函数
 * @param filters1 从宽到严的规则集
 * @param filters2 从严到宽的规则集
 * @return 最终选到的网络
 */
fun filterNetwork(
    filters1: Collection<NetFilter>,
    filters2: Collection<NetFilter>
): Collection<NetworkInterface> =
    NetworkInterface
        .getNetworkInterfaces()
        .toList()
        .filter(NetworkInterface::isUp)
        .tryFilter(filters1)
        .tryBest(filters2)

/**
 * 检查网络中是否包含特定成员
 * @param name 目标成员名字
 */
operator fun NetworkInterface.contains(name: String) =
    name in remoteHub("", this) refresh 600
