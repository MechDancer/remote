package org.mechdancer.version2.remote.functions.multicast

import org.mechdancer.remote.core.internal.Command
import org.mechdancer.remote.core.protocol.RemotePacket
import org.mechdancer.version2.dependency.AbstractModule
import org.mechdancer.version2.dependency.hashOf
import org.mechdancer.version2.dependency.maybe
import org.mechdancer.version2.dependency.must
import org.mechdancer.version2.remote.resources.MulticastSockets
import org.mechdancer.version2.remote.resources.Name
import org.mechdancer.version2.remote.resources.Name.Type.NAME
import java.net.DatagramPacket
import java.util.concurrent.atomic.AtomicLong

/**
 * 组播发布者
 */
class MulticastBroadcaster : AbstractModule() {
    private val name by maybe<Name> { host } // 可以匿名发送组播
    private val sockets by must<MulticastSockets> { host }

    private val serial = AtomicLong(0)

    fun broadcast(cmd: Command, payload: ByteArray = ByteArray(0)) {
        val packet =
            RemotePacket(
                command = cmd.id,
                sender = name?.get(NAME) ?: "",
                seqNumber = serial.getAndIncrement(),
                payload = payload
            )
                .bytes
                .let { DatagramPacket(it, it.size, sockets.address) }
        sockets.view.values.forEach { it.send(packet) }
    }

    override fun equals(other: Any?) = other is MulticastBroadcaster
    override fun hashCode() =
        TYPE_HASH

    private companion object {
        val TYPE_HASH = hashOf<MulticastBroadcaster>()
    }
}
