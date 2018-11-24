package org.mechdancer.framework.remote.protocol

/**
 * 通用数据包
 * 用于无连接通信或建立连接.
 *
 * @param command   指令识别号
 * @param sender    发送方名字
 * @param seqNumber 序列号
 * @param payload   数据负载
 */
class RemotePacket(
    val sender: String,
    val command: Byte,
    val seqNumber: Long,
    val payload: ByteArray
) {
    operator fun component1() = sender
    operator fun component2() = command
    operator fun component3() = seqNumber
    operator fun component4() = payload

    val bytes: ByteArray
        get() =
            SimpleOutputStream(sender.length * 2 + 1 + 1 + 9 + 9)
                .apply {
                    writeEnd(sender)
                    write(command.toInt())
                    zigzag(seqNumber, false)
                }
                .let { head ->
                    SimpleOutputStream(head.available() + payload.size)
                        .apply {
                            writeLength(head.core, 0, head.available())
                            write(payload)
                        }
                }
                .core
}
