package org.mechdancer.framework.remote.modules.tcpconnection

import org.mechdancer.framework.dependency.AbstractDependent
import org.mechdancer.framework.dependency.hashOf
import org.mechdancer.framework.remote.resources.TcpCmd.COMMON
import java.net.Socket

/**
 * 通用 TCP 服务器
 */
class CommonTcpServer(
    private val block: (String, Socket) -> Any?
) : AbstractDependent(), ShortConnectionListener {

    override val interest = COMMON.id

    override fun process(client: String, socket: Socket) {
        block(client, socket)
    }

    override fun equals(other: Any?) = other is CommonTcpServer
    override fun hashCode() = TYPE_HASH

    private companion object {
        val TYPE_HASH = hashOf<CommonTcpServer>()
    }
}
