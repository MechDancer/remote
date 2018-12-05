package org.mechdancer.framework.remote.modules.tcpconnection

import org.mechdancer.framework.dependency.AbstractDependent
import org.mechdancer.framework.remote.resources.TcpCmd.Dialog
import java.net.Socket

/**
 * TCP 应答服务器
 */
class DialogTcpServer(
    private val block: (String, ByteArray) -> ByteArray
) : AbstractDependent<DialogTcpServer>(DialogTcpServer::class),
    ShortConnectionListener {

    override val interest = Dialog.id

    override fun process(client: String, socket: Socket) =
        with(socket) { say(block(client, listen())) }
}
