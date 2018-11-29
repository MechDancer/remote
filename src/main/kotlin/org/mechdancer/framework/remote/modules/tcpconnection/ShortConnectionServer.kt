package org.mechdancer.framework.remote.modules.tcpconnection

import org.mechdancer.framework.dependency.AbstractModule
import org.mechdancer.framework.dependency.get
import org.mechdancer.framework.dependency.hashOf
import org.mechdancer.framework.dependency.must
import org.mechdancer.framework.remote.resources.ServerSockets
import org.mechdancer.framework.remote.resources.TcpCmd

/**
 * 短连接服务器
 */
class ShortConnectionServer : AbstractModule() {
    private val servers by must<ServerSockets>()
    private val mailListener = hashSetOf<MailListener>()
    private val connectionListeners = hashMapOf<Byte, ShortConnectionListener>()

    override fun sync() {
        synchronized(mailListener) {
            mailListener.clear()
            dependencies
                .get<MailListener>()
                .let(mailListener::addAll)
        }

        synchronized(connectionListeners) {
            connectionListeners.clear()
            dependencies
                .get<ShortConnectionListener>()
                .filterNot { it.interest == TcpCmd.Mail.id }
                .associate { it.interest to it }
                .let(connectionListeners::putAll)
        }
    }

    operator fun invoke(port: Int = 0) {
        servers[port]!!
            .accept()
            .use { socket ->
                val cmd = socket.listenCommand()
                val client = socket.listenString()
                when (cmd) {
                    TcpCmd.Mail.id -> {
                        val payload = socket.listen()
                        for (listener in mailListener)
                            listener.process(client, payload)
                    }
                    else           ->
                        connectionListeners[cmd]
                            ?.process(client, socket)
                }
            }
    }

    override fun equals(other: Any?) = other is ShortConnectionServer
    override fun hashCode() = TYPE_HASH

    private companion object {
        val TYPE_HASH = hashOf<ShortConnectionServer>()
    }
}
