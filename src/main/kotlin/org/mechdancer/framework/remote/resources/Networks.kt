package org.mechdancer.framework.remote.resources

import org.mechdancer.framework.dependency.ResourceFactory
import org.mechdancer.framework.dependency.hashOf
import java.net.Inet4Address
import java.net.NetworkInterface

/**
 * 网络端口扫描器
 */
class Networks : ResourceFactory<NetworkInterface, Inet4Address> {
    private val core = mutableMapOf<NetworkInterface, Inet4Address>()

    /**
     * 浏览网络端口和 IP 地址映射表
     */
    val view = object : Map<NetworkInterface, Inet4Address> by core {}

    /**
     * 扫描全部 IP 地址
     * 耗时为亚秒级，谨慎使用
     */
    fun scan() {
        val new = NetworkInterface
            .getNetworkInterfaces()
            .asSequence()
            .filter(NetworkInterface::isUp)
            .filter(NetworkInterface::supportsMulticast)
            .containsIpv4()
            .notLoopback()
            .notVirtual()
            .mapNotNull { network ->
                network.interfaceAddresses
                    .mapNotNull { it.address as? Inet4Address }
                    .singleOrNull()
                    ?.takeIf(::isMono)
                    ?.let { network to it }
            }

        synchronized(core) {
            core.clear()
            core.putAll(new)
        }
    }

    override fun get(parameter: NetworkInterface) = core[parameter]

    override fun equals(other: Any?) = other is Networks
    override fun hashCode() = TYPE_HASH

    private companion object {
        val TYPE_HASH = hashOf<Networks>()

        fun Sequence<NetworkInterface>.containsIpv4() =
            filter { network -> network.inetAddresses.toList().any { it is Inet4Address } }

        fun Sequence<NetworkInterface>.notLoopback() =
            filterNot { network -> network.isLoopback }

        fun Sequence<NetworkInterface>.notVirtual() =
            filterNot {
                fun check(it: String) = "virtual" in it.toLowerCase()
                it.isVirtual || check(it.name) || check(it.displayName)
            }

        fun isMono(it: Inet4Address) =
            it.address
                ?.first()
                ?.let { it + if (it >= 0) 0 else 256 }
                ?.takeIf { it in 1..223 && it != 127 } != null
    }
}