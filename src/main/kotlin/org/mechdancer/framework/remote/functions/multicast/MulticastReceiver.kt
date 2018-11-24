package org.mechdancer.framework.remote.functions.multicast

import org.mechdancer.framework.dependency.AbstractModule
import org.mechdancer.framework.dependency.get
import org.mechdancer.framework.dependency.hashOf
import org.mechdancer.framework.dependency.must
import org.mechdancer.framework.remote.protocol.RemotePacket
import org.mechdancer.framework.remote.protocol.SimpleInputStream
import org.mechdancer.framework.remote.protocol.readEnd
import org.mechdancer.framework.remote.protocol.zigzag
import org.mechdancer.framework.remote.resources.MulticastSockets
import org.mechdancer.framework.remote.resources.Name
import org.mechdancer.framework.remote.resources.Name.Type.NAME
import org.mechdancer.framework.remote.resources.UdpCmd
import java.net.DatagramPacket
import kotlin.concurrent.getOrSet

/**
 * 组播小包接收
 * @param bufferSize 缓冲区容量
 */
class MulticastReceiver(private val bufferSize: Int = 65536) : AbstractModule() {
    private val buffer = ThreadLocal<DatagramPacket>()        // 线程独立缓冲
    private val name by must<Name>(host)                      // 过滤环路数据
    private val socket by must<MulticastSockets>(host)        // 接收套接字
    private val callbacks = mutableSetOf<MulticastListener>() // 处理回调

    override fun sync() {
        callbacks.addAll(host().get())
    }

    operator fun invoke() =
        buffer
            .getOrSet { DatagramPacket(ByteArray(bufferSize), bufferSize) }
            .apply(socket.default::receive)
            .let { SimpleInputStream(it.data, it.length) }
            .run {
                readEnd().takeIf { it != name[NAME] }
                    ?.let {
                        RemotePacket(
                            sender = it,
                            command = read().toByte(),
                            seqNumber = zigzag(false),
                            payload = lookRest()
                        )
                    }
            }
            ?.also { pack ->
                callbacks
                    .filter { UdpCmd[pack.command] in it.interest }
                    .forEach { it process pack }
            }

    override fun equals(other: Any?) = other is MulticastReceiver
    override fun hashCode() = TYPE_HASH

    private companion object {
        val TYPE_HASH = hashOf<MulticastReceiver>()
    }
}
