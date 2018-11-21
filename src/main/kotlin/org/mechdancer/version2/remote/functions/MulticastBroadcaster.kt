package org.mechdancer.version2.remote.functions

import org.mechdancer.remote.core.internal.Command
import org.mechdancer.remote.core.protocol.RemotePackage
import org.mechdancer.version2.dependency.AbstractModule
import org.mechdancer.version2.hashOf
import org.mechdancer.version2.must
import org.mechdancer.version2.remote.resources.MulticastSockets
import org.mechdancer.version2.remote.resources.Name
import org.mechdancer.version2.remote.resources.Name.Type.NAME
import java.net.DatagramPacket

class MulticastBroadcaster : AbstractModule() {
    private val sockets by lazy { host.must<MulticastSockets>() }
    private val name by lazy { host.must<Name>() }

    fun broadcast(cmd: Command, payload: ByteArray = ByteArray(0)) {
        val packet =
            RemotePackage(cmd.id, name[NAME], payload).bytes
                .let { DatagramPacket(it, it.size, sockets.address) }
        sockets.view.values.forEach { it.send(packet) }
    }

    override fun equals(other: Any?) = other is MulticastBroadcaster
    override fun hashCode() = TYPE_HASH

    private companion object {
        val TYPE_HASH = hashOf<MulticastBroadcaster>()
    }
}
