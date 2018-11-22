package org.mechdancer.version2.remote.resources

import org.mechdancer.version2.dependency.ResourceMemory
import org.mechdancer.version2.hashOf
import java.net.InetAddress
import java.net.InetSocketAddress
import java.util.concurrent.ConcurrentHashMap

/**
 * 地址资源
 */
class Addresses : ResourceMemory<String, InetSocketAddress> {
    private val core = ConcurrentHashMap<String, InetSocketAddress>()

    /**
     * 仅更新 IP 地址
     * 以前不知道 IP 地址或新获知的 IP 地址与之前的不同，更新 IP 地址
     */
    fun update(parameter: String, ip: InetAddress?) =
        if (ip != null)
            core.compute(parameter) { _, last ->
                if (last?.address == ip) last
                else InetSocketAddress(ip, 0)
            }
        else core.remove(parameter)

    /**
     * 更新 IP
     */
    override fun update(parameter: String, resource: InetSocketAddress?) =
        if (resource != null) core.put(parameter, resource)
        else core.remove(parameter)

    /**
     * 仅获取 IP 地址
     */
    fun getIp(parameter: String) = core[parameter]?.address

    /**
     * 获取 IP 和端口，若端口为 0 视作地址未知
     */
    override fun get(parameter: String) = core[parameter]?.takeIf { it.port != 0 }

    override fun equals(other: Any?) = other is Addresses
    override fun hashCode() = TYPE_HASH

    private companion object {
        val TYPE_HASH = hashOf<Addresses>()
    }
}
