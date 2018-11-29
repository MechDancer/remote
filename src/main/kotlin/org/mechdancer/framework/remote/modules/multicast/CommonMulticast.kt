package org.mechdancer.framework.remote.modules.multicast

import org.mechdancer.framework.dependency.AbstractModule
import org.mechdancer.framework.dependency.hashOf
import org.mechdancer.framework.dependency.must
import org.mechdancer.framework.remote.protocol.RemotePacket
import org.mechdancer.framework.remote.resources.UdpCmd
import org.mechdancer.framework.remote.resources.UdpCmd.COMMON

/**
 * 通用组播协议
 * @param received 接收回调
 */
class CommonMulticast(
    private val received: (String, ByteArray) -> Unit
) : AbstractModule(), MulticastListener {
    private val broadcaster by must<MulticastBroadcaster>(dependencies)

    /**
     * 发布通用广播
     * @param payload 数据负载
     */
    infix fun broadcast(payload: ByteArray) = broadcaster.broadcast(UdpCmd.COMMON, payload)

    override val interest = INTEREST

    override fun process(remotePacket: RemotePacket) {
        val (name, _, payload) = remotePacket
        received(name, payload)
    }

    override fun equals(other: Any?) = other is CommonMulticast
    override fun hashCode() = TYPE_HASH

    private companion object {
        val TYPE_HASH = hashOf<CommonMulticast>()
        val INTEREST = setOf(COMMON.id)
    }
}
