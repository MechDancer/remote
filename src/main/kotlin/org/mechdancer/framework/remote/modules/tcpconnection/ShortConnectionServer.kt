package org.mechdancer.framework.remote.modules.tcpconnection

import org.mechdancer.framework.dependency.AbstractDependent
import org.mechdancer.framework.dependency.Component
import org.mechdancer.framework.dependency.hashOf
import org.mechdancer.framework.remote.modules.group.Rule
import org.mechdancer.framework.remote.resources.ServerSockets
import org.mechdancer.framework.remote.resources.TcpCmd
import org.mechdancer.framework.remote.resources.TcpFeedbackCmd
import kotlin.collections.set

/**
 * 短连接服务器
 */
class ShortConnectionServer(private val rule: Rule = Rule()) : AbstractDependent() {
    private val servers by must<ServerSockets>()
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
        servers[port]!!
            .accept()
            .use { socket ->
                val cmd = socket.listenCommand()
                val client = socket.listenString()

                if (rule decline client)
                    socket say TcpFeedbackCmd.DECLINE
                else when (cmd) {
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
