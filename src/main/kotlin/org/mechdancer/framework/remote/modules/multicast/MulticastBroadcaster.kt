package org.mechdancer.framework.remote.modules.multicast

import org.mechdancer.framework.dependency.AbstractDependent
import org.mechdancer.framework.remote.protocol.SimpleOutputStream
import org.mechdancer.framework.remote.protocol.writeEnd
import org.mechdancer.framework.remote.resources.Command
import org.mechdancer.framework.remote.resources.MulticastSockets
import org.mechdancer.framework.remote.resources.Name
import org.mechdancer.framework.remote.resources.UdpCmd
import org.mechdancer.framework.remote.resources.UdpCmd.ADDRESS_ACK
import org.mechdancer.framework.remote.resources.UdpCmd.YELL_ACK
import java.net.DatagramPacket

/**
 * 组播发布者
 */
class MulticastBroadcaster(size: Int = 0x4000) :
    AbstractDependent<MulticastBroadcaster>(MulticastBroadcaster::class) {

    private val name by maybe("") { it: Name -> it.field } // 可以匿名发送组播
    private val slicer by maybe<PacketSlicer>()
    private val sockets by must<MulticastSockets>()

    private val stub by lazy {
        SimpleOutputStream(size).apply { writeEnd(name) }
    }

    fun broadcast(cmd: Command, payload: ByteArray = ByteArray(0)) {
        if (name.isEmpty() && (cmd == YELL_ACK || cmd == ADDRESS_ACK)) return

        val stream = stub.clone()
        fun send() {
            val packet = DatagramPacket(stream.core, stream.ptr, sockets.address)
            for (socket in sockets.view.values)
                socket.send(packet)
        }

        when {
            // 一包能发
            stream.available() - 1 >= payload.size -> {
                stream.write(cmd.id)
                stream.write(payload)
                send()
            }
            // 可以分包
            slicer != null                         -> {
                stream.write(UdpCmd.PACKET_SLICE.id)
                val position = stream.ptr
                slicer!!.broadcast(cmd, payload, stream.available()) {
                    stream.ptr = position
                    stream.write(it)
                    send()
                }
            }
            // 否则
            else                                   ->
                throw RuntimeException("payload is too heavy!")
        }
    }
}
