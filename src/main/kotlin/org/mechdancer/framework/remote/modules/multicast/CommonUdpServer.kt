package org.mechdancer.framework.remote.modules.multicast

import org.mechdancer.framework.dependency.AbstractDependent
import org.mechdancer.framework.dependency.hashOf
import org.mechdancer.framework.remote.protocol.RemotePacket
import org.mechdancer.framework.remote.resources.UdpCmd.COMMON

/**
 * 通用组播协议，支持通用组播 [COMMON] = 127 的接收和发送
 *   发送依赖于 [MulticastBroadcaster]
 *   接收到将调用 [received]
 */
class CommonUdpServer(
    private val received: (String, ByteArray) -> Unit
) : AbstractDependent(), MulticastListener {
    private val broadcast by must { it: MulticastBroadcaster -> it::broadcast }

    fun broadcast(payload: ByteArray) = broadcast(COMMON, payload)

    override val interest = INTEREST

    override fun process(remotePacket: RemotePacket) =
        received(remotePacket.sender, remotePacket.payload)

    override fun equals(other: Any?) = other is CommonUdpServer
    override fun hashCode() = TYPE_HASH

    private companion object {
        val TYPE_HASH = hashOf<CommonUdpServer>()
        val INTEREST = setOf(COMMON.id)
    }
}
