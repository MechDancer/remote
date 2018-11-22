package org.mechdancer.remote.core.protocol

import java.io.*
import java.net.InetAddress
import java.net.InetSocketAddress

/**
 * 从字节数组恢复完整地址
 */
internal fun inetSocketAddress(byteArray: ByteArray) =
    byteArray
        .let(::ByteArrayInputStream)
        .readInetSocketAddress()

/**
 * 地址打包到字节数组
 */
internal val InetSocketAddress.bytes: ByteArray
    get() = ByteArrayOutputStream()
        .write(this)
        .toByteArray()

/**
 * 向流写入一个地址
 */
internal fun <T : OutputStream> T.write(address: InetSocketAddress) =
    apply {
        write(address.address.address)
        DataOutputStream(this).writeInt(address.port)
    }

/**
 * 从流读取一个地址
 */
internal fun InputStream.readInetSocketAddress() =
    let(::DataInputStream)
        .let {
            InetSocketAddress(
                InetAddress.getByAddress(it.waitNBytes(4)),
                it.readInt()
            )
        }
