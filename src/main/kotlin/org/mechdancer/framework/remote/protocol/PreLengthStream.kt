package org.mechdancer.framework.remote.protocol

import java.io.InputStream
import java.io.OutputStream

/**
 * 先将长度写入流，再将数据写入流。
 * @receiver 目标输出流
 * @param    pack 数据包
 */
infix fun OutputStream.writeWithLength(pack: ByteArray) {
    zigzag(pack.size.toLong(), false)
    write(pack)
}

/**
 * 先从流读出长度，再从流读出数据。
 * @receiver 输入流
 * @return   数据包
 */
fun InputStream.readWithLength(): ByteArray =
    waitNBytes(zigzag(false).toInt())

/**
 * 从输入流阻塞接收 [n] 个字节数据，或直到流关闭。
 * 函数会直接打开等于目标长度的缓冲区，因此不要用于实现尽量读取的功能。
 *
 * @receiver 源字节流
 * @param n 长度
 * @return 读取的字节数组
 */
infix fun InputStream.waitNBytes(n: Int): ByteArray {
    val buffer = SimpleOutputStream(n)
    for (i in 0 until n)
        read().takeIf { it in 0..255 }
            ?.also(buffer::write)
            ?: return buffer.core.copyOfRange(0, i)
    return buffer.core
}