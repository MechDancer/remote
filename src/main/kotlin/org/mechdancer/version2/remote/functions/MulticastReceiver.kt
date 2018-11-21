package org.mechdancer.version2.remote.functions

import org.mechdancer.remote.core.protocol.RemotePackage
import org.mechdancer.version2.dependency.AbstractModule
import org.mechdancer.version2.get
import org.mechdancer.version2.hashOf
import org.mechdancer.version2.must
import org.mechdancer.version2.remote.resources.MulticastSockets
import org.mechdancer.version2.remote.resources.Name
import org.mechdancer.version2.remote.resources.Name.Type.NAME
import java.net.DatagramPacket

/**
 * 组播小包接收
 * @param bufferSize 缓冲区容量
 */
class MulticastReceiver(private val bufferSize: Int = 65536) : AbstractModule() {
    private val socket by lazy { host.must<MulticastSockets>() }
    private val name by lazy { host.must<Name>() }
    private val callbacks = mutableSetOf<MulticastListener>()

    override fun sync() {
        callbacks.addAll(host.get())
    }

    operator fun invoke(): RemotePackage? {
        return DatagramPacket(ByteArray(bufferSize), bufferSize)
            .apply(socket.default::receive)
            .actualData
            .let { RemotePackage(it) }
            .takeIf { it.sender != name[NAME] }
            ?.also { pack -> callbacks.forEach { it process pack } }
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
