package org.mechdancer.framework.remote.protocol

import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.net.InetAddress
import java.net.InetSocketAddress

/**
 * 从字节数组恢复套接字地址
 */
internal fun inetSocketAddress(byteArray: ByteArray) =
    byteArray
        .let(::SimpleInputStream)
        .readInetSocketAddress()

/**
 * 套接字地址打包到字节数组
 */
internal val InetSocketAddress.bytes: ByteArray
    get() = SimpleOutputStream(8).write(this).core

/**
 * 从流读取一个套接字地址
 */
internal fun InputStream.readInetSocketAddress() =
    let(::DataInputStream)
        .let {
            InetSocketAddress(
                InetAddress.getByAddress(it.waitNBytes(4)),
                it.readInt()
            )
        }

/**
 * 向流写入一个套接字地址
 */
internal fun <T : OutputStream> T.write(address: InetSocketAddress) =
    apply {
        write(address.address.address)
        DataOutputStream(this).writeInt(address.port)
    }
