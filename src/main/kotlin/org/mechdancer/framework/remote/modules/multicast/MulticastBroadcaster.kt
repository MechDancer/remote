package org.mechdancer.framework.remote.modules.multicast

import org.mechdancer.framework.dependency.AbstractDependent
import org.mechdancer.framework.dependency.hashOf
import org.mechdancer.framework.remote.protocol.RemotePacket
import org.mechdancer.framework.remote.resources.Command
import org.mechdancer.framework.remote.resources.MulticastSockets
import org.mechdancer.framework.remote.resources.Name
import org.mechdancer.framework.remote.resources.UdpCmd.ADDRESS_ACK
import org.mechdancer.framework.remote.resources.UdpCmd.YELL_ACK
import java.net.DatagramPacket

/**
 * 组播发布者
 */
class MulticastBroadcaster : AbstractDependent() {
    private val name by maybe("") { it: Name -> it.field } // 可以匿名发送组播
    private val sockets by must<MulticastSockets>()

    fun broadcast(cmd: Command, payload: ByteArray = ByteArray(0)) {
        if (name.isEmpty() && (cmd == YELL_ACK || cmd == ADDRESS_ACK)) return

        val packet = RemotePacket(
            sender = name,
            command = cmd.id,
            payload = payload
        ).bytes.let { DatagramPacket(it, it.size, sockets.address) }

        for (socket in sockets.view.values)
            socket.send(packet)
    }

    override fun equals(other: Any?) = other is MulticastBroadcaster
    override fun hashCode() = TYPE_HASH

    private companion object {
        val TYPE_HASH = hashOf<MulticastBroadcaster>()
    }
}
