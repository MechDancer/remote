package org.mechdancer.framework.remote.modules.tcpconnection

import org.mechdancer.framework.dependency.AbstractDependent
import org.mechdancer.framework.dependency.Component
import org.mechdancer.framework.dependency.hashOf
import org.mechdancer.framework.remote.resources.ServerSockets
import org.mechdancer.framework.remote.resources.TcpCmd
import kotlin.collections.set

/**
 * 短连接服务器
 */
class ShortConnectionServer : AbstractDependent() {
    private val servers = must<ServerSockets>()
    private val mailListener = hashSetOf<MailListener>()
    private val listeners = hashMapOf<Byte, ShortConnectionListener>()

    override fun sync(dependency: Component): Boolean {
        super.sync(dependency)
        if (dependency is MailListener)
            mailListener.add(dependency)
        if (dependency is ShortConnectionListener)
            listeners[dependency.interest] = dependency
        return false
    }

    operator fun invoke(port: Int = 0) {
        servers
            .field[port]!!
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
                        listeners[cmd]?.process(client, socket)
                }
            }
    }

    override fun equals(other: Any?) = other is ShortConnectionServer
    override fun hashCode() = TYPE_HASH

    private companion object {
        val TYPE_HASH = hashOf<ShortConnectionServer>()
    }
}
