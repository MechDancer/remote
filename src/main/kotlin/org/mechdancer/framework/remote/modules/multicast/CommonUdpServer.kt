package org.mechdancer.framework.remote.modules.multicast

import org.mechdancer.framework.dependency.AbstractDependent
import org.mechdancer.framework.dependency.hashOf
import org.mechdancer.framework.remote.protocol.RemotePacket
import org.mechdancer.framework.remote.resources.UdpCmd.COMMON

/**
 * 通用组播协议
 * @param received 接收回调
 */
class CommonUdpServer(
    private val received: (String, ByteArray) -> Unit
) : AbstractDependent(), MulticastListener {
    override val interest = INTEREST

    override fun process(remotePacket: RemotePacket) {
        val (name, _, payload) = remotePacket
        received(name, payload)
    }

    override fun equals(other: Any?) = other is CommonUdpServer
    override fun hashCode() = TYPE_HASH

    private companion object {
        val TYPE_HASH = hashOf<CommonUdpServer>()
        val INTEREST = setOf(COMMON.id)
    }
}
