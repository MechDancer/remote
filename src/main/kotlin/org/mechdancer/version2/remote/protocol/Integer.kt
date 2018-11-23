package org.mechdancer.version2.remote.protocol

import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import kotlin.experimental.and

/**
 * 可变长编码
 * 在流上操作
 * @param num 要编码的数字
 * @param signed 是否带符号编码
 * @return 流式操作，返回本身
 */
fun <T : OutputStream> T.zigzag(
    num: Long,
    signed: Boolean
): T {
    var temp =
        if (signed) (num shl 1) xor (num shr 63)
        else num

    while (true)
        if (temp > 0x7f) {
            write((temp or 0x80).toInt())
            temp = temp ushr 7
        } else {
            write(temp.toInt())
            return this
        }
}

/**
 * 可变长解码
 * 在流上操作
 * @param signed 是否带符号解码
 * @return 数字
 */
infix fun InputStream.zigzag(signed: Boolean) =
    ByteArrayOutputStream(9)
        .apply {
            while (true)
                read()
                    .also(this::write)
                    .takeIf { it > 0x7f }
                    ?: break
        }
        .toByteArray() zigzag signed

/**
 * 编码一个数字
 * 不在流上操作
 * @param signed 是否带符号编码
 * @return 编码
 */
infix fun Long.zigzag(signed: Boolean): ByteArray {
    val buffer = ByteArray(9)

    var temp =
        if (signed) (this shl 1) xor (this shr 63)
        else this

    for (i in buffer.indices)
        if (temp > 0x7f) {
            buffer[i] = (temp or 0x80).toByte()
            temp = temp ushr 7
        } else {
            buffer[i] = temp.toByte()
            return buffer.copyOfRange(0, i + 1)
        }

    throw RuntimeException("impossible")
}

/**
 * 解码一个数字
 * 不在流上操作
 * @param signed 是否带符号解码
 * @return 数字
 */
infix fun ByteArray.zigzag(signed: Boolean): Long =
    foldRight(0L) { byte, acc ->
        acc shl 7 or ((byte and 0x7f).toLong())
    }.let {
        if (signed) (it ushr 1) xor -(it and 1)
        else it
    }
