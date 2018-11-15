package org.mechdancer.remote.util.network

import java.net.NetworkInterface

// 基于内在特征的筛选，必定是纯函数
/* ------------------------------------------------ *
 *     `name`    in JVM is    `name`     in Windows *
 * `displayName` in JVM is `description` in Windows *
 * ------------------------------------------------ */

fun NetworkInterface.wireless() =
    name.toLowerCase().startsWith("wlan")
        || displayName.toLowerCase().contains("wireless")

fun NetworkInterface.ethernet() =
    name.toLowerCase().startsWith("eth")
        || displayName.toLowerCase().contentEquals("ethernet")

fun NetworkInterface.virtual() =
    isVirtual
        || displayName.toLowerCase().contains("virtual")

operator fun NetFilter.not() = { net: NetworkInterface -> !this(net) }

val MULTICAST_FILTERS: List<NetFilter> =
    listOf(
        !NetworkInterface::isLoopback,
        NetworkInterface::supportsMulticast,
        !NetworkInterface::virtual
    )

val WIRELESS_FIRST: List<NetFilter> =
    listOf(
        NetworkInterface::wireless,
        NetworkInterface::ethernet,
        !NetworkInterface::virtual
    )
