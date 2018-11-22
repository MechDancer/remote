package org.mechdancer.remote.core.protocol

import java.io.InputStream
import java.io.OutputStream

/**
 * 先将长度写入流，再将数据写入流。
 * @receiver 目标输出流
 * @param    pack 数据包
 */
internal fun OutputStream.writeWithLength(pack: ByteArray) {
    zigzag(pack.size.toLong(), false)
    write(pack)
}

/**
 * 先从流读出长度，再从流读出数据。
 * @receiver 输入流
 * @return   数据包
 */
internal fun InputStream.readWithLength(): ByteArray =
    waitNBytes(zigzag(false).toInt())

/**
 * 从输入流阻塞接收 [n] 个字节数据，直到无法继续接收或流关闭。
 * 函数会直接打开等于目标长度的缓冲区，因此不要用于实现尽量读取的功能。
 *
 * @receiver 源字节流
 * @param n 长度
 * @return 读取的字节数组
 */
internal fun InputStream.waitNBytes(n: Int): ByteArray {
    val buffer = ByteArray(n)
    for (i in 0 until n) {
        buffer[i] = read()
            .takeIf { it in 0..255 }
            ?.toByte()
            ?: return buffer.copyOfRange(0, i)
    }
    return buffer
}
