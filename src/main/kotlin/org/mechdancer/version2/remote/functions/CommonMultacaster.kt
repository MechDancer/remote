package org.mechdancer.version2.remote.functions

import org.mechdancer.remote.core.protocol.RemotePackage
import org.mechdancer.version2.dependency.AbstractModule
import org.mechdancer.version2.hashOf
import org.mechdancer.version2.must
import org.mechdancer.version2.remote.resources.UdpCmd

/**
 * 通用组播协议
 */
class CommonMultacaster(
    private val received: (String, ByteArray) -> Unit
) : AbstractModule(), MulticastListener {
    private val broadcaster by lazy { host.must<MulticastBroadcaster>() }

    override val dependencies = setOf(MulticastBroadcaster::class, MulticastReceiver::class)

    override fun process(remotePackage: RemotePackage) {
        val (id, name, payload) = remotePackage
        if (id == UdpCmd.BROADCAST.id) received(name, payload)
    }

    infix fun broadcast(payload: ByteArray) = broadcaster.broadcast(UdpCmd.BROADCAST, payload)

    override fun equals(other: Any?) = other is CommonMultacaster
    override fun hashCode() = TYPE_HASH

    private companion object {
        val TYPE_HASH = hashOf<CommonMultacaster>()
    }
}
