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
    val command: Byte,
    val sender: String,
    val seqNumber: Long,
    val neck: ByteArray,
    val payload: ByteArray
) {
    operator fun component1() = command
    operator fun component2() = sender
    operator fun component3() = seqNumber
    operator fun component4() = neck
    operator fun component5() = payload

    val bytes: ByteArray
        get() =
            SimpleOutputStream(1 + sender.length * 2 + 1 + 9 + 9 + neck.size)
                .apply {
                    write(command.toInt())
                    writeEnd(sender)
                    zigzag(seqNumber, false)
                    writeWithLength(neck)
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
