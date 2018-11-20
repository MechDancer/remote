package org.mechdancer.version2.dependency.resource.basic

import org.mechdancer.version2.dependency.ResourceFactory
import org.mechdancer.version2.hashOf
import java.net.InetSocketAddress
import java.net.MulticastSocket
import java.net.NetworkInterface
import java.util.concurrent.ConcurrentHashMap

/**
 * 组播套接字资源
 */
class MulticastSockets(
    val address: InetSocketAddress
) : ResourceFactory<NetworkInterface, MulticastSocket> {
    private val core = ConcurrentHashMap<NetworkInterface, MulticastSocket>()
    val view = object : Map<NetworkInterface, MulticastSocket> by core {}

    val default = multicastOn(address, null)

    override operator fun get(parameter: NetworkInterface): MulticastSocket =
        core[parameter] ?: multicastOn(address, parameter).also { core[parameter] = it }

    operator fun get(timeout: Int): MulticastSocket =
        multicastOn(address, null).apply { soTimeout = timeout }

    override fun equals(other: Any?) = other is MulticastSockets
    override fun hashCode() = TYPE_HASH

    private companion object {
        val TYPE_HASH = hashOf<MulticastSockets>()

        // 构造组播套接字
        fun multicastOn(group: InetSocketAddress, net: NetworkInterface?) =
            MulticastSocket(group.port).apply {
                net?.let(this::setNetworkInterface)
                joinGroup(group.address)
            }
    }
}
