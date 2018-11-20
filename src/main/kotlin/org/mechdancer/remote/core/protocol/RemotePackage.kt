package org.mechdancer.remote.core.protocol

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

/**
 * 通用数据包
 * 用于无连接通信或建立连接.
 *
 * @param command 指令识别号
 * @param sender  发送方名字
 * @param payload 数据负载
 */
data class RemotePackage(
    val command: Byte,
    val sender: String,
    val payload: ByteArray
) {
    /**
     * 打包到字节数组
     */
    val bytes by lazy {
        ByteArrayOutputStream().apply {
            write(command.toInt())
            writeEnd(sender)
            write(payload)
        }.toByteArray()
    }

    companion object {
        /**
         * 从字节数组构建
         */
        operator fun invoke(pack: ByteArray) =
            pack.let(::ByteArrayInputStream)
                .let {
                    val cmd = it.read().toByte()
                    val sender = it.readEnd()
                    val payload = it.readBytes()
                    RemotePackage(cmd, sender, payload)
                }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as RemotePackage

        if (command != other.command) return false
        if (sender != other.sender) return false
        if (!payload.contentEquals(other.payload)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = command.toInt()
        result = 31 * result + sender.hashCode()
        result = 31 * result + payload.contentHashCode()
        return result
    }
}
