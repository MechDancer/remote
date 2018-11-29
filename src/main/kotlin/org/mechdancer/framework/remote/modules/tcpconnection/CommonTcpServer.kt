package org.mechdancer.framework.remote.modules.tcpconnection

import org.mechdancer.framework.dependency.AbstractModule
import org.mechdancer.framework.dependency.hashOf
import org.mechdancer.framework.remote.resources.TcpCmd.COMMON
import java.net.Socket

/**
 * 通用 TCP 服务器
 */
class CommonTcpServer(
    private val block: Socket.() -> Any?
) : AbstractModule(), ShortConnectionListener {

    override val interest = INTEREST

    override fun process(socket: Socket) {
        block(socket)
    }

    override fun equals(other: Any?) = other is CommonTcpServer
    override fun hashCode() = TYPE_HASH

    private companion object {
        val TYPE_HASH = hashOf<CommonTcpServer>()
        val INTEREST = setOf(COMMON.id)
    }
}
