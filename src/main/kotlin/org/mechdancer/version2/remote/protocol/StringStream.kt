package org.mechdancer.version2.remote.protocol

import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream

internal fun OutputStream.writeEnd(string: String) {
    write(string.toByteArray())
    write(0)
}

internal fun InputStream.readEnd(): String {
    val buffer = ByteArrayOutputStream()
    while (true) {
        when (val temp = read()) {
            -1, 0 -> return String(buffer.toByteArray())
            else  -> buffer.write(temp)
        }
    }
}
