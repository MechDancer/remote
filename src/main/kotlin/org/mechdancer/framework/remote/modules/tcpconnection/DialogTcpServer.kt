package org.mechdancer.framework.remote.modules.tcpconnection

import org.mechdancer.framework.dependency.AbstractModule
import org.mechdancer.framework.dependency.hashOf
import org.mechdancer.framework.remote.resources.TcpCmd.Dialog
import java.net.Socket

/**
 * TCP 应答服务器
 */
class DialogTcpServer(
    private val block: (String, ByteArray) -> ByteArray
) : AbstractModule(), ShortConnectionListener {

    override val interest = Dialog.id

    override fun process(client: String, socket: Socket) =
        with(socket) { say(block(client, listen())) }

    override fun equals(other: Any?) = other is DialogTcpServer
    override fun hashCode() = TYPE_HASH

    private companion object {
        val TYPE_HASH = hashOf<DialogTcpServer>()
    }
}