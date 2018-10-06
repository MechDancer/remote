package org.mechdancer.remote.console.s4

import org.mechdancer.remote.console.parser.Token

object DefaultParser {
    private val buffers = setOf(
        NumberBuffer(),
        WordBuffer(),
        StringBuffer(),
        SignBuffer(),
        KeyBuffer(),
        NoteBuffer()
    )
    private val sentence = mutableListOf<Token<*>>()

    private fun summary(char: Char? = null) {
        buffers
            .maxBy { it.size }
            ?.build()
            ?.let { sentence += it }
        buffers.forEach { it.reset(char) }
    }

    operator fun invoke(source: String): List<Token<*>> {
        sentence.clear()
        for (char in source)
            if (buffers.map { it.offer(char) }.none { it })
                summary(char)
        summary()
        return sentence.toList()
    }
}

fun main(args: Array<String>) {
    val source = "adfg (* int *)  12345.76  +-67890 0xff  \"hello world\" (- this is a note "
    DefaultParser(source).forEach { println("${it.type} : $it") }
}
