package org.mechdancer.version2.remote.functions

import org.mechdancer.remote.core.protocol.RemotePacket
import org.mechdancer.version2.dependency.AbstractModule
import org.mechdancer.version2.dependency.get
import org.mechdancer.version2.dependency.hashOf
import org.mechdancer.version2.dependency.must
import org.mechdancer.version2.remote.resources.MulticastSockets
import org.mechdancer.version2.remote.resources.Name
import org.mechdancer.version2.remote.resources.Name.Type.NAME
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
            ?.also { pack -> callbacks.forEach { it process pack } }

    override fun equals(other: Any?) = other is MulticastReceiver
    override fun hashCode() = TYPE_HASH

    private companion object {
        val TYPE_HASH = hashOf<MulticastReceiver>()

        // 拆解 UDP 数据包
        val DatagramPacket.actualData
            get() = data.copyOfRange(0, length)
    }
}
