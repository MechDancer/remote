package org.mechdancer.framework.remote.resources

import org.mechdancer.framework.dependency.ResourceFactory
import org.mechdancer.framework.dependency.buildView
import org.mechdancer.framework.dependency.hashOf
import java.net.Inet4Address
import java.net.NetworkInterface

/**
 * 网络端口扫描器
 */
class Networks : ResourceFactory<NetworkInterface, Inet4Address> {
    private val core = mutableMapOf<NetworkInterface, Inet4Address>()
    val view = buildView(core)

    init {
        scan()
    }

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
            .notLoopback()
            .notVirtual()
            .mapNotNull { network ->
                network.interfaceAddresses
                    .mapNotNull { it.address as? Inet4Address }
                    .singleOrNull(::isMono)
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

        fun Sequence<NetworkInterface>.notLoopback() =
            filterNot { network -> network.isLoopback }

        fun Sequence<NetworkInterface>.notVirtual() =
            filterNot {
                fun check(it: String) = "virtual" in it.toLowerCase()
                it.isVirtual || check(it.name) || check(it.displayName)
            }

        fun isMono(it: Inet4Address) =
            it.address
                .first()
                .toInt()
                .and(0xff)
                .let { it in 1..223 && it != 127 }
    }
}
