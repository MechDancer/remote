package org.mechdancer.remote.core.network

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

fun NetworkInterface.notVirtual() =
    isVirtual
        || displayName.toLowerCase().contains("virtual")

val multicastFilters: List<NetFilter> =
    listOf(
        NetworkInterface::supportsMulticast,
        NetworkInterface::notVirtual
    )

val wirelessFirst: List<NetFilter> =
    listOf(
        NetworkInterface::wireless,
        NetworkInterface::ethernet,
        NetworkInterface::notVirtual,
        NetworkInterface::isLoopback
    )