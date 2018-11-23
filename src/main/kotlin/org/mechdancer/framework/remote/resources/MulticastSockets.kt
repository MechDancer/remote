package org.mechdancer.framework.remote.resources

import org.mechdancer.framework.dependency.ResourceFactory
import org.mechdancer.framework.dependency.hashOf
import java.net.InetSocketAddress
import java.net.MulticastSocket
import java.net.NetworkInterface
import java.util.concurrent.ConcurrentHashMap

/**
 * 组播套接字资源
 * @param address 组地址
 */
class MulticastSockets(
    val address: InetSocketAddress
) : ResourceFactory<NetworkInterface, MulticastSocket> {
    private val core = ConcurrentHashMap<NetworkInterface, MulticastSocket>()

    /**
     * 浏览所有组播套接字
     */
    val view = object : Map<NetworkInterface, MulticastSocket> by core {}

    /**
     * 默认套接字
     * 接收所有到达本机的组播包
     * 不要修改组、超时等状态
     */
    val default by lazy { multicastOn(address, null) }

    /**
     * 获取经由特定网络端口的组播套接字
     */
    override operator fun get(parameter: NetworkInterface): MulticastSocket =
        core.computeIfAbsent(parameter) { multicastOn(address, parameter) }

    /**
     * 获取指定超时时间的临时套接字
     */
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
