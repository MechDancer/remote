package org.mechdancer.framework.remote.protocol

import java.io.ByteArrayOutputStream

/**
 * 通用数据包
 * 用于无连接通信或建立连接.
 *
 * @param command   指令识别号
 * @param sender    发送方名字
 * @param seqNumber 序列号
 * @param payload   数据负载
 */
data class RemotePacket(
    val command: Byte,
    val sender: String,
    val seqNumber: Long,
    val payload: ByteArray
) {
    /**
     * 打包到字节数组
     */
    val bytes
        get() = ByteArrayOutputStream()
            .apply {
                write(command.toInt())
                writeEnd(sender)
                zigzag(seqNumber, false)
                write(payload)
            }
            .toByteArray()

    companion object {
        /**
         * 从字节数组构建
         */
        operator fun invoke(pack: ByteArray) =
            pack.let(::SimpleInputStream)
                .let {
                    val cmd = it.read().toByte()
                    val sender = it.readEnd()
                    val seqNumber = it zigzag false
                    val payload = it.readBytes()
                    RemotePacket(cmd, sender, seqNumber, payload)
                }
    }

    override fun equals(other: Any?) =
        this === other
            || (other is RemotePacket
            && command == other.command
            && sender == other.sender
            && seqNumber == other.seqNumber
            && payload.contentEquals(other.payload))

    override fun hashCode(): Int {
        var result = command.toInt()
        result = 31 * result + sender.hashCode()
        result = 31 * result + seqNumber.hashCode()
        return 31 * result + payload.contentHashCode()
    }
}
