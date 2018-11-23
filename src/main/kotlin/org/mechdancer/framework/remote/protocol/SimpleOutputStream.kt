package org.mechdancer.framework.remote.protocol

import java.io.OutputStream

class SimpleOutputStream(size: Int) : OutputStream() {
    val core = ByteArray(size)
    private var ptr = 0

    override infix fun write(b: Int) {
        core[ptr++] = b.toByte()
    }

    override infix fun write(byteArray: ByteArray) {
        byteArray.copyInto(core, ptr)
        ptr += byteArray.size
    }

    fun writeLength(byteArray: ByteArray, begin: Int, length: Int) {
        byteArray.copyInto(core, ptr, begin, begin + length)
        ptr += length
    }

    fun writeRange(byteArray: ByteArray, begin: Int, end: Int) {
        byteArray.copyInto(core, ptr, begin, end)
        ptr += end - begin
    }

    fun writeFrom(stream: SimpleInputStream, length: Int) {
        stream.readInto(this, length)
    }

    override fun close() {
        ptr = core.size
    }
}
