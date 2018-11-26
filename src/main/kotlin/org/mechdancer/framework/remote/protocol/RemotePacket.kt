package org.mechdancer.framework.remote.protocol

import java.net.DatagramPacket
import java.net.InetSocketAddress

/**
 * 通用数据包
 * 用于无连接通信或建立连接.
 *
 * @param command   指令识别号
 * @param sender    发送方名字
 * @param serial 序列号
 * @param payload   数据负载
 */
class RemotePacket(
    val sender: String,
    val command: Byte,
    val serial: Long,
    val payload: ByteArray
) {
    operator fun component1() = sender
    operator fun component2() = command
    operator fun component3() = serial
    operator fun component4() = payload

    fun toDatagramPacket(address: InetSocketAddress) =
        SimpleOutputStream(sender.length * 2 + 1 + 1 + 9 + payload.size)
            .apply {
                writeEnd(sender)       // 每个字符最多占 2 字节，1 个字节结尾
                write(command.toInt()) // 1 个字节指令
                zigzag(serial, false)  // 1 个变长整数，最多 9 字节
                write(payload)         // 剩下写入负载
            }
            .let {
                DatagramPacket(it.core, it.available(), address)
            }

    override fun toString() =
        "sender: $sender, cmd: $command, serial: $serial, payload: byte[${payload.size}]"
}
