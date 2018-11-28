package org.mechdancer.framework.remote.protocol

/**
 * 通用数据包
 * 用于无连接通信或建立连接
 *
 * @param command   指令识别号
 * @param sender    发送方名字
 * @param payload   数据负载
 */
class RemotePacket(
    val sender: String,
    val command: Byte,
    val payload: ByteArray
) {
    operator fun component1() = sender
    operator fun component2() = command
    operator fun component3() = payload

    val bytes: ByteArray
        get() {
            val name = sender.toByteArray()
            return SimpleOutputStream(name.size + 1 + 1 + payload.size)
                .apply {
                    write(name)       // 每个字符最多占 2 字节，1 个字节结尾
                    write(0)
                    write(command.toInt()) // 1 个字节指令
                    write(payload)         // 剩下写入负载
                }
                .core
        }

    override fun toString() =
        "sender: $sender, cmd: $command, payload: byte[${payload.size}]"
}
