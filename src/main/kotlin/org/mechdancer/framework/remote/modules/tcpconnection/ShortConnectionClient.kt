package org.mechdancer.framework.remote.modules.tcpconnection

import org.mechdancer.framework.dependency.AbstractModule
import org.mechdancer.framework.dependency.hashOf
import org.mechdancer.framework.dependency.maybe
import org.mechdancer.framework.dependency.must
import org.mechdancer.framework.remote.resources.Addresses
import org.mechdancer.framework.remote.resources.Command
import org.mechdancer.framework.remote.resources.Name
import java.net.Socket
import java.net.SocketException

/**
 * 短连接客户端
 */
class ShortConnectionClient : AbstractModule() {
    private val name by must<Name>(dependencies)
    private val addresses by must<Addresses>(dependencies)
    private val monitor by maybe<PortMonitor>(dependencies)

    /**
     * 连接一个远端
     * @param server 远端名字
     */
    fun connect(server: String, cmd: Command): Socket? {
        val address =
            addresses[server] ?: run {
                monitor?.ask(server)
                return null
            }

        val socket = Socket()
        return try {
            socket.also { I ->
                I.connect(address)
                I say cmd
                I say name.value
            }
        } catch (e: SocketException) {
            addresses remove server
            monitor?.ask(server)
            null
        }
    }

    override fun equals(other: Any?) = other is ShortConnectionClient
    override fun hashCode() = TYPE_HASH

    private companion object {
        val TYPE_HASH = hashOf<ShortConnectionClient>()
    }
}
