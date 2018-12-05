package org.mechdancer.framework.remote.modules.multicast

import org.mechdancer.framework.dependency.AbstractComponent
import org.mechdancer.framework.remote.protocol.RemotePacket
import org.mechdancer.framework.remote.resources.UdpCmd.COMMON

/**
 * 通用组播协议，支持接收通用组播 [COMMON] = 127
 * 收到将调用 [received]
 */
class CommonUdpServer(
    private val received: (String, ByteArray) -> Unit
) : AbstractComponent<CommonUdpServer>(CommonUdpServer::class),
    MulticastListener {

    override val interest = INTEREST

    override fun process(remotePacket: RemotePacket) =
        received(remotePacket.sender, remotePacket.payload)

    private companion object {
        val INTEREST = setOf(COMMON.id)
    }
}
