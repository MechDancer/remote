package org.mechdancer.version2.dependency.functions.basic

import org.mechdancer.remote.core.internal.Command
import org.mechdancer.remote.core.protocol.RemotePackage
import org.mechdancer.version2.dependency.Dependency
import org.mechdancer.version2.dependency.FunctionModule
import org.mechdancer.version2.dependency.resource.basic.HostInfo
import org.mechdancer.version2.dependency.resource.basic.HostInfo.Type.Name
import org.mechdancer.version2.dependency.resource.basic.MulticastSockets
import org.mechdancer.version2.hashOf
import org.mechdancer.version2.must
import java.net.DatagramPacket

class MulticastBroadcaster : FunctionModule {
    private lateinit var sockets: MulticastSockets
    private lateinit var hostInfo: HostInfo

    override val dependencies get() = setOf(sockets, hostInfo)
    override fun loadDependencies(all: Iterable<Dependency>) {
        sockets = all.must()
        hostInfo = all.must()
    }

    fun broadcast(cmd: Command, payload: ByteArray = ByteArray(0)) {
        val packet =
            RemotePackage(cmd.id, hostInfo[Name], payload)
                .bytes
                .let { DatagramPacket(it, it.size, sockets.address) }
        sockets.view.values.forEach { it.send(packet) }
    }

    override fun equals(other: Any?) = other is MulticastBroadcaster
    override fun hashCode() = TYPE_HASH

    private companion object {
        val TYPE_HASH = hashOf<MulticastBroadcaster>()
    }
}
