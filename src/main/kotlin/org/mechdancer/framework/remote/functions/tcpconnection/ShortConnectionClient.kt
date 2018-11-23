package org.mechdancer.framework.remote.functions.tcpconnection

import org.mechdancer.framework.dependency.AbstractModule
import org.mechdancer.framework.dependency.hashOf
import org.mechdancer.framework.dependency.maybe
import org.mechdancer.framework.dependency.must
import org.mechdancer.framework.remote.functions.address.AddressMonitor
import org.mechdancer.framework.remote.resources.Addresses
import java.net.InetSocketAddress
import java.net.Socket
import java.net.SocketException

/**
 * 短连接客户端
 */
class ShortConnectionClient : AbstractModule() {
    private val addresses by must<Addresses> { host }
    private val addressMonitor by maybe<AddressMonitor> { host }

    /**
     * 连接一个远端
     * @param name 远端名字
     */
    infix fun connect(name: String): Socket? {
        val address = addresses[name] ?: return null
        val socket = Socket()
        try {
            socket.connect(address)
        } catch (e: SocketException) {
            addresses.update(name, null as InetSocketAddress?)
            addressMonitor?.ask(name)
            return null
        }
        return socket
    }

    override fun equals(other: Any?) = other is ShortConnectionClient
    override fun hashCode() = TYPE_HASH

    private companion object {
        val TYPE_HASH = hashOf<ShortConnectionClient>()
    }
}
