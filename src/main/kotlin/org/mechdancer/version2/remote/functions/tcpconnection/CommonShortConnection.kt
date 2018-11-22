package org.mechdancer.version2.remote.functions.tcpconnection

import org.mechdancer.version2.dependency.AbstractModule
import org.mechdancer.version2.dependency.hashOf
import org.mechdancer.version2.remote.resources.TcpCmd.COMMON
import java.net.Socket

/**
 * 通用组播接收
 */
class CommonShortConnection(
    private val block: Socket.() -> Any?
) : AbstractModule(), ShortConnectionListener {

    override val interest = INTEREST

    override fun process(socket: Socket) {
        block(socket)
    }

    override fun equals(other: Any?) = other is CommonShortConnection
    override fun hashCode() = TYPE_HASH

    private companion object {
        val TYPE_HASH = hashOf<CommonShortConnection>()
        val INTEREST = setOf(COMMON)
    }
}
