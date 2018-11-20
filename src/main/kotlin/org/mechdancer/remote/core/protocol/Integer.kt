package org.mechdancer.remote.core.protocol

import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import kotlin.experimental.and

/**
 * 符号折叠
 */
fun signEnhash(long: Long) = (long shl 1) xor (long shr 63)

/**
 * 符号展开
 */
fun signDehash(long: Long) = (long ushr 1) xor -(long and 1)

/**
 * 可变长编码
 */
fun OutputStream.enZigzag(num: Long) {
    var temp = num
    while (true)
        if (temp > 0x7f) {
            write((temp or 0x80).toInt())
            temp = temp ushr 7
        } else {
            write(temp.toInt())
            return
        }
}

/**
 * 可变长解码
 */
fun InputStream.deZigzag() =
    ByteArrayOutputStream()
        .apply {
            while (true)
                read()
                    .also(this::write)
                    .takeIf { it > 0x7f }
                    ?: break
        }
        .toByteArray()
        .foldRight(0L) { byte, acc ->
            acc shl 7 or ((byte and 0x7f).toLong())
        }
