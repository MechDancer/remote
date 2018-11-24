package org.mechdancer.framework.remote.functions.multicast

import org.mechdancer.framework.dependency.AbstractModule
import org.mechdancer.framework.dependency.hashOf
import org.mechdancer.framework.dependency.maybe
import org.mechdancer.framework.dependency.must
import org.mechdancer.framework.remote.protocol.RemotePacket
import org.mechdancer.framework.remote.resources.Command
import org.mechdancer.framework.remote.resources.MulticastSockets
import org.mechdancer.framework.remote.resources.Name
import org.mechdancer.framework.remote.resources.Name.Type.NAME
import java.net.DatagramPacket
import java.util.concurrent.atomic.AtomicLong

/**
 * 组播发布者
 */
class MulticastBroadcaster : AbstractModule() {
    private val name by maybe<Name>(host) // 可以匿名发送组播
    private val sockets by must<MulticastSockets>(host)

    private val serial = AtomicLong(0)

    fun broadcast(cmd: Command, payload: ByteArray = ByteArray(0)) =
        RemotePacket(
            command = cmd.id,
            sender = name?.get(NAME) ?: "",
            seqNumber = serial.getAndIncrement(),
            neck = ByteArray(0),
            payload = payload
        )
            .bytes
            .let { DatagramPacket(it, it.size, sockets.address) }
            .let { packet ->
                sockets.view.values.forEach { it.send(packet) }
            }

    override fun equals(other: Any?) = other is MulticastBroadcaster
    override fun hashCode() = TYPE_HASH

    private companion object {
        val TYPE_HASH = hashOf<MulticastBroadcaster>()
    }
}
