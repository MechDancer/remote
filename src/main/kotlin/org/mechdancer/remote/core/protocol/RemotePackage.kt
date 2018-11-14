package org.mechdancer.remote.core.protocol

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream

/**
 * 通用数据包
 * 用于无连接通信或建立连接.
 *
 * @param command 指令识别号
 * @param sender  发送方名字
 * @param payload 数据负载
 */
internal class RemotePackage(
    val command: Byte,
    val sender: String,
    val payload: ByteArray
) {
    /**
     * 打包到字节数组
     */
    val bytes by lazy {
        ByteArrayOutputStream().apply {
            DataOutputStream(this).apply {
                writeByte(command.toInt())
                writeWithLength(sender.toByteArray())
            }
            write(payload)
        }.toByteArray()
    }

    operator fun component1() = command
    operator fun component2() = sender
    operator fun component3() = payload

    companion object {
        /**
         * 从字节数组构建
         */
        operator fun invoke(pack: ByteArray) =
            pack.let(::ByteArrayInputStream)
                .let(::DataInputStream)
                .let {
                    val cmd = it.readByte()
                    val sender = String(it.readWithLength())
                    val payload = it.readBytes()
                    RemotePackage(cmd, sender, payload)
                }
    }
}
