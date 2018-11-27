package org.mechdancer.framework.remote.resources

import org.mechdancer.framework.dependency.Dependency
import org.mechdancer.framework.dependency.hashOf
import java.net.InetAddress
import java.net.InetSocketAddress
import java.util.concurrent.ConcurrentHashMap

/**
 * 地址资源
 * 记录成员的地址和端口
 */
class Addresses : Dependency {

    private val core = ConcurrentHashMap<String, InetSocketAddress>()

    operator fun set(name: String, address: InetAddress) =
        core.compute(name) { _, last ->
            InetSocketAddress(address, last?.port ?: 0)
        }

    operator fun set(name: String, port: Int) =
        core.compute(name) { _, last ->
            InetSocketAddress(
                last?.address ?: InetAddress.getByAddress(ByteArray(0)),
                port
            )
        }

    operator fun set(name: String, socket: InetSocketAddress) =
        core.put(name, socket)

    operator fun get(name: String) =
        core[name]?.takeUnless { it.port == 0 }

    infix fun remove(name: String) {
        core.remove(name)
    }

    override fun equals(other: Any?) = other is Addresses
    override fun hashCode() = TYPE_HASH

    private companion object {
        val TYPE_HASH = hashOf<Addresses>()
    }
}
