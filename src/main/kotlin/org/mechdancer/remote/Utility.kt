package org.mechdancer.remote

import java.io.InputStream
import kotlin.math.min

fun InputStream.readNBytes(len: Int): ByteArray {
    val count = min(available(), len)
    val buffer = ByteArray(count)
    read(buffer, 0, count)
    return buffer
}