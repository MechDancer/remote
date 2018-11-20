package org.mechdancer.version2.dependency.functions.basic

import org.mechdancer.remote.core.protocol.RemotePackage
import org.mechdancer.version2.dependency.Dependency
import org.mechdancer.version2.dependency.FunctionModule
import org.mechdancer.version2.dependency.resource.basic.HostInfo
import org.mechdancer.version2.dependency.resource.basic.HostInfo.Type.Name
import org.mechdancer.version2.dependency.resource.basic.MulticastSockets
import org.mechdancer.version2.get
import org.mechdancer.version2.hashOf
import org.mechdancer.version2.must
import java.net.DatagramPacket

/**
 * 组播小包接收
 * @param bufferSize 缓冲区容量
 */
class MulticastReceiver(private val bufferSize: Int = 65536) : FunctionModule {
    private lateinit var hostInfo: HostInfo
    private lateinit var socket: MulticastSockets
    private val callbacks = mutableListOf<(RemotePackage) -> Unit>()

    override val dependencies: Set<Dependency>
        get() = setOf(socket)

    override fun loadDependencies(dependency: Iterable<Dependency>) {
        hostInfo = dependency.must()
        socket = dependency.must()
        callbacks.addAll(
            dependency
                .get<MulticastListener>()
                .map { it::invoke }
        )
    }

    operator fun invoke(): RemotePackage? =
        DatagramPacket(ByteArray(bufferSize), bufferSize)
            .apply(socket.default::receive)
            .actualData
            .let { RemotePackage(it) }
            .takeIf { it.sender != hostInfo[Name] }
            ?.also { pack -> callbacks.forEach { it(pack) } }

    override fun equals(other: Any?) = other is MulticastReceiver
    override fun hashCode() = TYPE_HASH

    private companion object {
        val TYPE_HASH = hashOf<MulticastReceiver>()

        // 拆解 UDP 数据包
        val DatagramPacket.actualData
            get() = data.copyOfRange(0, length)
    }
}
