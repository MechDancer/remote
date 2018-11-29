package org.mechdancer.framework.remote.modules.tcpconnection

import org.mechdancer.framework.dependency.AbstractModule
import org.mechdancer.framework.dependency.hashOf
import org.mechdancer.framework.dependency.maybe
import org.mechdancer.framework.dependency.must
import org.mechdancer.framework.remote.resources.Addresses
import org.mechdancer.framework.remote.resources.Command
import java.net.Socket
import java.net.SocketException

/**
 * 短连接客户端
 */
class ShortConnectionClient : AbstractModule() {
    private val addresses by must<Addresses>(host)
    private val monitor by maybe<PortMonitor>(host)

    /**
     * 连接一个远端
     * @param name 远端名字
     */
    fun connect(name: String, cmd: Command): Socket? {
        val address =
            addresses[name] ?: run {
                monitor?.ask(name)
                return null
            }

        val socket = Socket()
        return try {
            socket.also { I ->
                I.connect(address)
                I say cmd
            }
        } catch (e: SocketException) {
            addresses remove name
            monitor?.ask(name)
            null
        }
    }

    override fun equals(other: Any?) = other is ShortConnectionClient
    override fun hashCode() = TYPE_HASH

    private companion object {
        val TYPE_HASH = hashOf<ShortConnectionClient>()
    }
}
