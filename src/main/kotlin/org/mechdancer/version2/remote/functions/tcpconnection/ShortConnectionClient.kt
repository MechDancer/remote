package org.mechdancer.version2.remote.functions.tcpconnection

import org.mechdancer.version2.dependency.AbstractModule
import org.mechdancer.version2.dependency.hashOf
import org.mechdancer.version2.dependency.maybe
import org.mechdancer.version2.dependency.must
import org.mechdancer.version2.remote.functions.address.AddressMonitor
import org.mechdancer.version2.remote.resources.Addresses
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
