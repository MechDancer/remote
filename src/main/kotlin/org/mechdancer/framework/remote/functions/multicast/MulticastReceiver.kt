package org.mechdancer.framework.remote.functions.multicast

import org.mechdancer.framework.dependency.AbstractModule
import org.mechdancer.framework.dependency.get
import org.mechdancer.framework.dependency.hashOf
import org.mechdancer.framework.dependency.must
import org.mechdancer.framework.remote.protocol.RemotePacket
import org.mechdancer.framework.remote.resources.MulticastSockets
import org.mechdancer.framework.remote.resources.Name
import org.mechdancer.framework.remote.resources.Name.Type.NAME
import org.mechdancer.framework.remote.resources.UdpCmd
import java.net.DatagramPacket

/**
 * 组播小包接收
 * @param bufferSize 缓冲区容量
 */
class MulticastReceiver(private val bufferSize: Int = 65536) : AbstractModule() {
    private val socket by must<MulticastSockets> { host }
    private val name by must<Name> { host }
    private val callbacks = mutableSetOf<MulticastListener>()

    override fun sync() {
        callbacks.addAll(host.get())
    }

    operator fun invoke() =
        DatagramPacket(ByteArray(bufferSize), bufferSize)
            .apply(socket.default::receive)
            .actualData
            .let { RemotePacket(it) }
            .takeIf { it.sender != name[NAME] }
            ?.also { pack ->
                callbacks
                    .filter { UdpCmd[pack.command] in it.interest }
                    .forEach { it process pack }
            }

    override fun equals(other: Any?) = other is MulticastReceiver
    override fun hashCode() = TYPE_HASH

    private companion object {
        val TYPE_HASH = hashOf<MulticastReceiver>()

        // 拆解 UDP 数据包
        val DatagramPacket.actualData
            get() = data.copyOfRange(0, length)
    }
}
