package org.mechdancer.framework.remote.modules.multicast

import org.mechdancer.framework.dependency.AbstractModule
import org.mechdancer.framework.dependency.hashOf
import org.mechdancer.framework.dependency.maybe
import org.mechdancer.framework.dependency.must
import org.mechdancer.framework.remote.protocol.RemotePacket
import org.mechdancer.framework.remote.resources.MulticastSockets
import org.mechdancer.framework.remote.resources.Name
import org.mechdancer.framework.remote.resources.UdpCmd.ADDRESS_ACK
import org.mechdancer.framework.remote.resources.UdpCmd.YELL_ACK
import java.util.concurrent.atomic.AtomicLong

/**
 * 组播发布者
 */
class MulticastBroadcaster : AbstractModule() {
    private val name by maybe<Name>(host) // 可以匿名发送组播
    private val sockets by must<MulticastSockets>(host)

    private val serial = AtomicLong(0)

    fun broadcast(cmd: Byte, payload: ByteArray = ByteArray(0)) {
        val me = name?.value?.trim() ?: ""

        if (me.isEmpty() && (cmd == YELL_ACK.id || cmd == ADDRESS_ACK.id)) return

        val packet = RemotePacket(
            sender = me,
            command = cmd,
            serial = serial.getAndIncrement(),
            payload = payload
        ).toDatagramPacket(sockets.address)

        for (socket in sockets.view.values)
            socket.send(packet)
    }

    override fun equals(other: Any?) = other is MulticastBroadcaster
    override fun hashCode() = TYPE_HASH

    private companion object {
        val TYPE_HASH = hashOf<MulticastBroadcaster>()
    }
}
