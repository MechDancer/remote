package org.mechdancer.framework.remote.modules.tcpconnection

import org.mechdancer.framework.dependency.AbstractDependent
import org.mechdancer.framework.dependency.hashOf
import org.mechdancer.framework.remote.resources.Addresses
import org.mechdancer.framework.remote.resources.Command
import org.mechdancer.framework.remote.resources.Name
import java.net.Socket
import java.net.SocketException

/**
 * 短连接客户端
 */
class ShortConnectionClient : AbstractDependent() {
    private val name = must<Name>()
    private val addresses = must<Addresses>()
    private val monitor = maybe<PortMonitor>()

    /**
     * 连接一个远端
     * @param server 远端名字
     */
    fun connect(server: String, cmd: Command): Socket? {
        val address =
            addresses.field[server] ?: run {
                monitor.field?.ask(server)
                return null
            }

        val socket = Socket()
        return try {
            socket.also { I ->
                I.connect(address)
                I say cmd
                I say name.field.value
            }
        } catch (e: SocketException) {
            addresses.field remove server
            monitor.field?.ask(server)
            null
        }
    }

    override fun equals(other: Any?) = other is ShortConnectionClient
    override fun hashCode() = TYPE_HASH

    private companion object {
        val TYPE_HASH = hashOf<ShortConnectionClient>()
    }
}
