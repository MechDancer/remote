package org.mechdancer.version2.remote.functions.tcpconnection

import org.mechdancer.version2.dependency.AbstractModule
import org.mechdancer.version2.dependency.get
import org.mechdancer.version2.dependency.hashOf
import org.mechdancer.version2.dependency.must
import org.mechdancer.version2.remote.resources.ServerSockets
import org.mechdancer.version2.remote.resources.TcpCmd

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

    fun listen() = listen(0)

    infix fun listen(port: Int) {
        servers[port]!!
            .accept()
            .use { socket ->
                socket
                    .getInputStream()
                    .read()
                    .toByte()
                    .let { cmd -> listeners.singleOrNull { TcpCmd[cmd] in it.interest } }
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
