package org.mechdancer.framework.remote.network

import java.net.Inet4Address
import java.net.InetAddress
import java.net.NetworkInterface

// 基于内在特征的筛选，必定是纯函数
/* ------------------------------------------------ *
 *     `name`    in JVM is    `name`     in Windows *
 * `displayName` in JVM is `description` in Windows *
 * ------------------------------------------------ */

fun InetAddress.isV4Host() =
    (this as? Inet4Address)
        ?.address
        ?.first()
        ?.let { it + if (it >= 0) 0 else 256 }
        ?.takeIf { it in 1..223 } != null

fun NetworkInterface.wireless() =
    name.toLowerCase().startsWith("wlan")
        || displayName.toLowerCase().contains("wireless")

fun NetworkInterface.ethernet() =
    name.toLowerCase().startsWith("eth")
        || displayName.toLowerCase().contentEquals("ethernet")

fun NetworkInterface.virtual() =
    isVirtual
        || displayName.toLowerCase().contains("virtual")

fun NetworkInterface.containsV4Host() =
    inetAddresses.toList().any(InetAddress::isV4Host)

operator fun NetFilter.not() = { net: NetworkInterface -> !this(net) }

val MULTICAST_FILTERS: List<NetFilter> =
    listOf(
        !NetworkInterface::isLoopback,
        NetworkInterface::supportsMulticast,
        NetworkInterface::containsV4Host,
        !NetworkInterface::virtual
    )

val WIRELESS_FIRST: List<NetFilter> =
    listOf(
        NetworkInterface::wireless,
        NetworkInterface::ethernet,
        !NetworkInterface::virtual
    )
