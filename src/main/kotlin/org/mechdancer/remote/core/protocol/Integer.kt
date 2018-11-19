package org.mechdancer.remote.core.protocol

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import kotlin.experimental.and

fun ByteArrayOutputStream.enZigzag(num: Long) {
    var temp = (num shl 1) xor (num shr 63)
    while (true)
        if (temp > 0x7f) {
            write((temp or 0x80).toInt())
            temp = temp ushr 7
        } else {
            write(temp.toInt())
            return
        }
}

fun ByteArrayInputStream.deZigzag() =
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
        .let { (it ushr 1) xor -(it and 1) }

fun main(args: Array<String>) {
    ByteArrayOutputStream()
        .apply { enZigzag(-123456) }
        .toByteArray()
        .let(::ByteArrayInputStream)
        .deZigzag()
        .let(::println)
}