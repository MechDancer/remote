package org.mechdancer.framework.remote.modules.tcpconnection

import org.mechdancer.framework.dependency.AbstractModule
import org.mechdancer.framework.dependency.hashOf
import org.mechdancer.framework.remote.resources.TcpCmd.Dialog
import java.net.Socket

/**
 * TCP 应答服务器
 */
class DialogTcpServer(
    private val block: (ByteArray) -> ByteArray
) : AbstractModule(), ShortConnectionListener {

    override val interest = INTEREST

    override fun process(socket: Socket) =
        with(socket) { say(block(listen())) }

    override fun equals(other: Any?) = other is DialogTcpServer
    override fun hashCode() = TYPE_HASH

    private companion object {
        val TYPE_HASH = hashOf<DialogTcpServer>()
        val INTEREST = setOf(Dialog.id)
    }
}
