package org.mechdancer.remote.core.network

import java.net.NetworkInterface

typealias NetFilter = Predicate<NetworkInterface>

/**
 * 选网函数
 * @param filters1 从宽到严的规则集
 * @param filters2 从严到宽的规则集
 * @return 最终选到的网络
 */
fun selectNetwork(
    filters1: Collection<Predicate<NetworkInterface>>,
    filters2: Collection<Predicate<NetworkInterface>>
) = NetworkInterface.getNetworkInterfaces().toList()
    .filter(NetworkInterface::isUp) filter filters1 first filters2