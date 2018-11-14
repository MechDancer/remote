package org.mechdancer.remote.core.network

import java.net.NetworkInterface
import kotlin.streams.toList

/**
 * 选网规则
 */
typealias NetFilter = (NetworkInterface) -> Boolean

/**
 * 从严选网原则
 * 顺序使用一套从宽到严的规则尽量精细选择
 *
 * * 没有可选的网络或仅剩一个候选网络，立即返回
 * * 还有多个候选网络但选网规则已用尽，立即返回
 * * 应用最高级选网规则筛选并递归
 */
tailrec infix fun Collection<NetworkInterface>.filter(
    filters: Collection<NetFilter>
): Collection<NetworkInterface> =
    takeIf { it.size < 2 }
        ?: takeIf { filters.isEmpty() }
        ?: filter(filters.first()) filter filters.drop(1)

/**
 * 从优选网原则
 * 顺序使用一套从严到宽的规则尽量最优选择
 *
 * * 只要使用某个规则筛后有网络存在，立即返回其中一个
 * * 否则尝试匹配下一条规则
 */
infix fun Collection<NetworkInterface>.first(
    filters: Collection<NetFilter>
): NetworkInterface? {
    for (filter in filters)
        return firstOrNull(filter) ?: continue
    return null
}

/**
 * 选网函数
 * @param filters1 从宽到严的规则集
 * @param filters2 从严到宽的规则集
 * @return 最终选到的网络
 */
fun selectNetwork(
    filters1: Collection<NetFilter>,
    filters2: Collection<NetFilter>
) = NetworkInterface
    .networkInterfaces()
    .filter(NetworkInterface::isUp)
    .toList()
    .filter(filters1)
    .first(filters2)
