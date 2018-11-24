package org.mechdancer.framework.remote.protocol

import java.io.InputStream

class SimpleInputStream(
    val core: ByteArray,
    val size: Int = core.size
) : InputStream() {
    private var ptr = 0

    override fun available() = size - ptr

    override fun read() =
        if (ptr < size)
            core[ptr++].let { if (it >= 0) it.toInt() else it + 256 }
        else -1

    fun look() = core[ptr]

    infix fun skip(length: Int) = also { ptr += length }

    fun lookRest() = lookUntil(size)

    infix fun lookUntil(length: Int): ByteArray {
        val result = ByteArray(length - ptr)
        core.copyInto(result, 0, ptr, length)
        return result
    }

    fun readInto(stream: SimpleOutputStream, length: Int) {
        stream.writeLength(core, ptr, length)
        ptr += length
    }

    override fun close() {
        ptr = size
    }
}
