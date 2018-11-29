package org.mechdancer.framework.remote.modules.tcpconnection

import org.mechdancer.framework.dependency.AbstractModule
import org.mechdancer.framework.dependency.hashOf
import org.mechdancer.framework.dependency.must
import org.mechdancer.framework.remote.resources.ServerSockets

/**
 * 短连接服务器
 */
class ShortConnectionServer : AbstractModule() {
    private val servers by must<ServerSockets>(dependencies)
    private val listeners = hashMapOf<Byte, ShortConnectionListener>()

    override fun sync() {
        synchronized(listeners) {
            dependencies()
                .mapNotNull { it as? ShortConnectionListener }
                .associate { it.interest to it }
                .let(listeners::putAll)
        }
    }

    operator fun invoke(port: Int = 0) {
        servers[port]!!
            .accept()
            .use { socket ->
                val cmd = socket.listenCommand()
                val client = String(socket.listen())
                listeners[cmd]?.process(client, socket)
            }
    }

    override fun equals(other: Any?) = other is ShortConnectionServer
    override fun hashCode() = TYPE_HASH

    private companion object {
        val TYPE_HASH = hashOf<ShortConnectionServer>()
    }
}
