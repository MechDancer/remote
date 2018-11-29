package org.mechdancer.framework.remote.modules.multicast

import org.mechdancer.framework.dependency.*
import org.mechdancer.framework.remote.protocol.RemotePacket
import org.mechdancer.framework.remote.protocol.SimpleInputStream
import org.mechdancer.framework.remote.protocol.readEnd
import org.mechdancer.framework.remote.resources.Addresses
import org.mechdancer.framework.remote.resources.MulticastSockets
import org.mechdancer.framework.remote.resources.Name
import org.mechdancer.framework.remote.resources.Networks
import java.net.DatagramPacket
import java.net.Inet4Address
import java.net.InterfaceAddress
import kotlin.concurrent.getOrSet

/**
 * 组播小包接收
 * @param bufferSize 缓冲区容量
 */
class MulticastReceiver(private val bufferSize: Int = 65536) : AbstractModule() {
    private val buffer = ThreadLocal<DatagramPacket>()        // 线程独立缓冲
    private val name by maybe<Name>()                         // 过滤环路数据
    private val sockets by must<MulticastSockets>()           // 接收套接字
    private val listeners = mutableSetOf<MulticastListener>() // 处理回调

    private val networks by maybe<Networks>()   // 网络管理
    private val addresses by maybe<Addresses>() // 地址管理

    override fun sync() {
        synchronized(listeners) {
            listeners.clear()
            listeners.addAll(dependencies.get())
        }
    }

    operator fun invoke(): RemotePacket? {
        val packet = buffer
            .getOrSet { DatagramPacket(ByteArray(bufferSize), bufferSize) }
            .apply(sockets.default::receive)

        val stream = SimpleInputStream(core = packet.data, end = packet.length)
        val sender = stream.readEnd()

        if (sender == name?.value ?: "") return null

        val address = packet.address as Inet4Address

        networks
            ?.view
            ?.toList()
            ?.find { (_, it) -> it match address }
            ?.let { (network, _) -> sockets[network] }

        addresses?.set(sender, address)

        return RemotePacket(
            sender = sender,
            command = stream.read().toByte(),
            payload = stream.lookRest()
        ).also { pack ->
            listeners
                .filter { pack.command in it.interest }
                .forEach { it process pack }
        }
    }

    override fun equals(other: Any?) = other is MulticastReceiver
    override fun hashCode() = TYPE_HASH

    private companion object {
        val TYPE_HASH = hashOf<MulticastReceiver>()

        fun Inet4Address.toInt() = address
            .fold(0) { acc, byte -> (acc shl 8) or (byte.toInt() and 0xff) }

        infix fun InterfaceAddress.match(other: Inet4Address): Boolean {
            if (address == other) return true
            val mask = Int.MAX_VALUE shl 32 - networkPrefixLength.toInt()
            return other.toInt() and mask == (address as Inet4Address).toInt() and mask
        }
    }
}