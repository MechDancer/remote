package org.mechdancer.framework.remote.streams

import java.io.InputStream

class SimpleInputStream(val core: ByteArray) : InputStream() {
    private var ptr = 0

    override fun available() = core.size - ptr

    override fun read() =
        if (ptr < core.size)
            core[ptr++].let { if (it >= 0) it.toInt() else it + 256 }
        else -1

    fun look() = core[ptr]

    fun skip(length: Int) = also { ptr += length }

    fun lookRest(): ByteArray {
        val result = ByteArray(core.size - ptr)
        core.copyInto(result, 0, ptr, core.size)
        return result
    }

    fun readInto(stream: SimpleOutputStream, length: Int) {
        stream.writeLength(core, ptr, length)
        ptr += length
    }

    override fun close() {
        ptr = core.size
    }
}
