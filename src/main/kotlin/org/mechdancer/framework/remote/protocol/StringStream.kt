package org.mechdancer.framework.remote.protocol

import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream

/**
 * 向流写入字符串再写入结尾
 */
fun OutputStream.writeEnd(string: String) =
    apply {
        write(string.toByteArray())
        write(0)
    }

/**
 * 从流读取一个带结尾的字符串
 */
fun InputStream.readEnd(): String {
    val buffer = ByteArrayOutputStream()
    while (true)
        when (val temp = read()) {
            -1, 0 -> return String(buffer.toByteArray())
            else  -> buffer.write(temp)
        }
}
