package org.mechdancer.framework.remote.functions.tcpconnection

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
    private val servers by must<ServerSockets> { host }
    private val listeners = mutableSetOf<ShortConnectionListener>()

    override fun sync() {
        listeners
            .apply { addAll(host.get()) }
            .map { it.interest }
            .run { flatten().distinct().size == sumBy { it.size } }
            .let { if (!it) throw RuntimeException(REDUPLICATE_ERROR_MSG) }
    }

    operator fun invoke(port: Int = 0) {
        servers[port]!!
            .accept()
            .use { socket ->
                socket
                    .invoke { TcpCmd[it]!! }
                    .let { cmd -> listeners.singleOrNull { cmd in it.interest } }
                    ?.process(socket)
            }
    }

    override fun equals(other: Any?) = other is ShortConnectionServer
    override fun hashCode() = TYPE_HASH

    private companion object {
        val TYPE_HASH = hashOf<ShortConnectionServer>()
        const val REDUPLICATE_ERROR_MSG = "more than one listener interested in same command"
    }
}
