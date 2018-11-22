package org.mechdancer.version2.remote.functions.commons

import org.mechdancer.remote.core.protocol.RemotePacket
import org.mechdancer.version2.dependency.AbstractModule
import org.mechdancer.version2.dependency.hashOf
import org.mechdancer.version2.dependency.must
import org.mechdancer.version2.remote.functions.MulticastBroadcaster
import org.mechdancer.version2.remote.functions.MulticastListener
import org.mechdancer.version2.remote.resources.UdpCmd

/**
 * 通用组播协议
 * @param received 接收回调
 */
class CommonMulticast(
    private val received: (String, ByteArray) -> Unit
) : AbstractModule(), MulticastListener {
    private val broadcaster by lazy { host.must<MulticastBroadcaster>() }

    override fun process(remotePacket: RemotePacket) {
        val (id, name, _, payload) = remotePacket
        if (id == UdpCmd.BROADCAST.id) received(name, payload)
    }

    /**
     * 发布通用广播
     * @param payload 数据负载
     */
    infix fun broadcast(payload: ByteArray) = broadcaster.broadcast(UdpCmd.BROADCAST, payload)

    override fun equals(other: Any?) = other is CommonMulticast
    override fun hashCode() = TYPE_HASH

    private companion object {
        val TYPE_HASH = hashOf<CommonMulticast>()
    }
}
