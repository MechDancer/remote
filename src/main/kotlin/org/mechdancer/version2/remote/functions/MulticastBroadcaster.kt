package org.mechdancer.version2.remote.functions

import org.mechdancer.remote.core.internal.Command
import org.mechdancer.remote.core.protocol.RemotePacket
import org.mechdancer.version2.dependency.AbstractModule
import org.mechdancer.version2.hashOf
import org.mechdancer.version2.must
import org.mechdancer.version2.remote.resources.MulticastSockets
import org.mechdancer.version2.remote.resources.Name
import org.mechdancer.version2.remote.resources.Name.Type.NAME
import java.net.DatagramPacket
import java.util.concurrent.atomic.AtomicLong

class MulticastBroadcaster : AbstractModule() {
    private val sockets by lazy { host.must<MulticastSockets>() }
    private val name by lazy { host.must<Name>() }
    private val serial = AtomicLong(0)

    fun broadcast(cmd: Command, payload: ByteArray = ByteArray(0)) {
        val packet =
            RemotePacket(
                cmd.id,
                name[NAME],
                serial.getAndIncrement(),
                payload
            ).bytes
                .let { DatagramPacket(it, it.size, sockets.address) }
        sockets.view.values.forEach { it.send(packet) }
    }

    override fun equals(other: Any?) = other is MulticastBroadcaster
    override fun hashCode() = TYPE_HASH

    private companion object {
        val TYPE_HASH = hashOf<MulticastBroadcaster>()
    }
}
