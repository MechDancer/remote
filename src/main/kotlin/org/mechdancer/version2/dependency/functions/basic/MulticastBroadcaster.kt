package org.mechdancer.version2.dependency.functions.basic

import org.mechdancer.remote.core.internal.Command
import org.mechdancer.remote.core.protocol.RemotePackage
import org.mechdancer.version2.dependency.AbstractModule
import org.mechdancer.version2.dependency.resources.basic.MulticastSockets
import org.mechdancer.version2.dependency.resources.basic.Name
import org.mechdancer.version2.dependency.resources.basic.Name.Type.NAME
import org.mechdancer.version2.hashOf
import org.mechdancer.version2.must
import java.net.DatagramPacket

class MulticastBroadcaster : AbstractModule() {
    private val sockets by lazy { host.must<MulticastSockets>() }
    private val name by lazy { host.must<Name>() }

    override val dependencies get() = setOf(MulticastSockets::class, Name::class)

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
